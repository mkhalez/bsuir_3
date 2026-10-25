"""Поиск документа на фотографии, выравнивание и очистка."""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Optional

import cv2
import numpy as np
from PIL import Image, ImageOps

MAX_DETECT_SIZE = 1400
MIN_AREA_RATIO = 0.05


@dataclass
class ScanResult:
    """Результат обработки фотографии."""

    image: np.ndarray
    corners: np.ndarray
    found: bool


def load_image(path: str) -> np.ndarray:
    """Загружает изображение с учётом EXIF-поворота и юникод-путей."""
    with Image.open(path) as pil_image:
        pil_image = ImageOps.exif_transpose(pil_image)
        rgb = np.asarray(pil_image.convert("RGB"))
    return cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)


def save_image(path: str, image: np.ndarray, quality: int = 95) -> None:
    """Сохраняет BGR-изображение, корректно работая с юникод-путями."""
    extension = os.path.splitext(path)[1].lower() or ".png"
    params = []
    if extension in (".jpg", ".jpeg"):
        params = [cv2.IMWRITE_JPEG_QUALITY, int(quality)]
    ok, buffer = cv2.imencode(extension, image, params)
    if not ok:
        raise ValueError(f"Не удалось сохранить изображение в формате {extension}")
    buffer.tofile(path)


def order_points(points: np.ndarray) -> np.ndarray:
    """Упорядочивает 4 точки: левый верх, правый верх, правый низ, левый низ."""
    pts = np.asarray(points, dtype=np.float32).reshape(4, 2)
    rect = np.zeros((4, 2), dtype=np.float32)

    sums = pts.sum(axis=1)
    rect[0] = pts[np.argmin(sums)]
    rect[2] = pts[np.argmax(sums)]

    diffs = np.diff(pts, axis=1).ravel()
    rect[1] = pts[np.argmin(diffs)]
    rect[3] = pts[np.argmax(diffs)]
    return rect


def _approx_quad(contour: np.ndarray) -> Optional[np.ndarray]:
    perimeter = cv2.arcLength(contour, True)
    if perimeter <= 0:
        return None

    for epsilon in (0.02, 0.03, 0.04, 0.06):
        approx = cv2.approxPolyDP(contour, epsilon * perimeter, True)
        if len(approx) == 4 and cv2.isContourConvex(approx):
            return approx.reshape(4, 2).astype(np.float32)
    return None


def _best_quad(
    mask: np.ndarray, image_area: float, max_area_ratio: float = 1.01
) -> Optional[np.ndarray]:
    contours, _ = cv2.findContours(mask, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:50]

    best_quad = None
    best_area = 0.0
    for contour in contours:
        if cv2.contourArea(contour) < image_area * MIN_AREA_RATIO:
            continue

        quad = _approx_quad(contour)
        if quad is None:
            continue

        quad_area = abs(float(cv2.contourArea(quad)))
        ratio = quad_area / image_area
        if not (MIN_AREA_RATIO <= ratio <= max_area_ratio):
            continue

        if quad_area > best_area:
            best_area = quad_area
            best_quad = quad

    return best_quad


def detect_document_corners(image: np.ndarray) -> Optional[np.ndarray]:
    """Находит углы самого крупного четырёхугольника (документа) на фото."""
    if image is None or image.size == 0:
        return None

    height, width = image.shape[:2]
    scale = min(1.0, MAX_DETECT_SIZE / float(max(height, width)))
    if scale < 1.0:
        small = cv2.resize(
            image, None, fx=scale, fy=scale, interpolation=cv2.INTER_AREA
        )
    else:
        small = image

    gray = cv2.cvtColor(small, cv2.COLOR_BGR2GRAY)
    gray = cv2.GaussianBlur(gray, (5, 5), 0)
    image_area = float(gray.shape[0] * gray.shape[1])

    median = float(np.median(gray))
    lower = int(max(0, 0.66 * median))
    upper = int(min(255, max(lower + 1, 1.33 * median)))

    edges = cv2.Canny(gray, lower, upper)
    edges = cv2.morphologyEx(
        edges, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8), iterations=2
    )
    edges = cv2.dilate(edges, np.ones((3, 3), np.uint8), iterations=1)

    _, otsu = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    otsu_inverted = cv2.bitwise_not(otsu)

    quad = _best_quad(edges, image_area)
    if quad is None:
        quad = _best_quad(otsu, image_area, max_area_ratio=0.97)
    if quad is None:
        quad = _best_quad(otsu_inverted, image_area, max_area_ratio=0.97)
    if quad is None:
        return None

    if scale < 1.0:
        quad = quad / scale

    quad[:, 0] = np.clip(quad[:, 0], 0, width - 1)
    quad[:, 1] = np.clip(quad[:, 1], 0, height - 1)
    return quad.astype(np.float32)


def warp_document(image: np.ndarray, corners: np.ndarray) -> np.ndarray:
    """Выпрямляет документ по четырём углам (перспективное преобразование)."""
    rect = order_points(corners)
    top_left, top_right, bottom_right, bottom_left = rect

    width = int(
        max(
            np.linalg.norm(top_right - top_left),
            np.linalg.norm(bottom_right - bottom_left),
        )
    )
    height = int(
        max(
            np.linalg.norm(bottom_left - top_left),
            np.linalg.norm(bottom_right - top_right),
        )
    )
    width = max(width, 1)
    height = max(height, 1)

    destination = np.array(
        [[0, 0], [width - 1, 0], [width - 1, height - 1], [0, height - 1]],
        dtype=np.float32,
    )
    matrix = cv2.getPerspectiveTransform(rect, destination)
    return cv2.warpPerspective(
        image, matrix, (width, height), flags=cv2.INTER_CUBIC
    )


def _remove_shadows(image: np.ndarray) -> np.ndarray:
    """Выравнивает освещение и убирает тени, оценивая фон страницы."""
    height, width = image.shape[:2]
    scale = min(1.0, 700.0 / float(max(height, width)))

    if scale < 1.0:
        small = cv2.resize(
            image, None, fx=scale, fy=scale, interpolation=cv2.INTER_AREA
        )
    else:
        small = image

    shortest = min(small.shape[:2])
    kernel_size = max(15, (shortest // 15) | 1)
    kernel_size = min(kernel_size, 101)
    if kernel_size % 2 == 0:
        kernel_size += 1

    kernel = cv2.getStructuringElement(
        cv2.MORPH_RECT, (kernel_size, kernel_size)
    )

    backgrounds = []
    for channel in cv2.split(small.astype(np.float32)):
        background = cv2.morphologyEx(channel, cv2.MORPH_CLOSE, kernel)
        background = cv2.GaussianBlur(
            background, (0, 0), sigmaX=max(2.0, kernel_size / 3.0)
        )
        backgrounds.append(background)

    background_small = cv2.merge(backgrounds)
    if scale < 1.0:
        background = cv2.resize(
            background_small, (width, height), interpolation=cv2.INTER_LINEAR
        )
    else:
        background = background_small

    background_channels = cv2.split(background)
    corrected_channels = []
    for channel, channel_background in zip(
        cv2.split(image.astype(np.float32)), background_channels
    ):
        corrected = channel * 255.0 / np.maximum(channel_background, 1.0)
        corrected_channels.append(
            np.clip(corrected, 0, 255).astype(np.uint8)
        )

    return cv2.merge(corrected_channels)


def enhance_document(
    image: np.ndarray, bw: bool = False, remove_shadows: bool = True
) -> np.ndarray:
    """Делает документ «чистым»: убирает тени, повышает контраст или делает скан."""
    result = _remove_shadows(image) if remove_shadows else image.copy()

    if bw:
        gray = cv2.cvtColor(result, cv2.COLOR_BGR2GRAY)
        gray = cv2.bilateralFilter(gray, 5, 40, 40)
        _, binary = cv2.threshold(
            gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU
        )
        return cv2.cvtColor(binary, cv2.COLOR_GRAY2BGR)

    lab = cv2.cvtColor(result, cv2.COLOR_BGR2LAB)
    lightness, channel_a, channel_b = cv2.split(lab)
    clahe = cv2.createCLAHE(clipLimit=1.5, tileGridSize=(8, 8))
    lightness = clahe.apply(lightness)
    return cv2.cvtColor(
        cv2.merge((lightness, channel_a, channel_b)), cv2.COLOR_LAB2BGR
    )


def process(
    image: np.ndarray, bw: bool = False, remove_shadows: bool = True
) -> ScanResult:
    """Полный цикл: поиск контура -> выпрямление -> очистка."""
    corners = detect_document_corners(image)
    found = corners is not None

    if corners is None:
        height, width = image.shape[:2]
        corners = np.array(
            [[0, 0], [width - 1, 0], [width - 1, height - 1], [0, height - 1]],
            dtype=np.float32,
        )

    warped = warp_document(image, corners)
    if min(warped.shape[:2]) < 2:
        warped = image.copy()

    cleaned = enhance_document(warped, bw=bw, remove_shadows=remove_shadows)
    return ScanResult(image=cleaned, corners=corners, found=found)
