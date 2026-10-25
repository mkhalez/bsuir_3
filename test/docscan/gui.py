"""Настольное приложение DocScan на Tkinter."""

from __future__ import annotations

import os
import queue
import sys
import threading
import tkinter as tk
import tkinter.font as tkfont
from tkinter import filedialog, messagebox, ttk
from typing import Optional

import cv2
from PIL import Image, ImageTk

from .scanner import ScanResult, load_image, process, save_image

try:
    from tkinterdnd2 import DND_FILES, TkinterDnD
except Exception:  # библиотека необязательна
    DND_FILES = None
    TkinterDnD = None


class ScannerApp(ttk.Frame):
    PREVIEW_WIDTH = 500
    PREVIEW_HEIGHT = 580

    def __init__(self, master: tk.Misc) -> None:
        super().__init__(master, padding=(16, 12))
        self.master = master
        self.source_image: Optional[cv2.Mat] = None
        self.source_path: Optional[str] = None
        self.result: Optional[ScanResult] = None
        self.mode = tk.StringVar(value="color")
        self.remove_shadows_var = tk.BooleanVar(value=True)
        self.status_var = tk.StringVar(
            value="Откройте фото с документом или перетащите файл в окно."
        )

        self._busy = False
        self._job_id = 0
        self._photos: dict[tk.Canvas, ImageTk.PhotoImage] = {}
        self._canvas_images: dict[tk.Canvas, cv2.Mat] = {}
        self._canvas_messages: dict[tk.Canvas, str] = {}
        self._resize_jobs: dict[tk.Canvas, str] = {}
        self._font_family = tkfont.nametofont("TkDefaultFont").actual("family")

        self._build_ui()
        self._update_controls()
        self._setup_dnd()

    def _build_ui(self) -> None:
        self.master.title("DocScan — документ по контуру")
        self.master.geometry("1100x800")
        self.master.minsize(960, 700)

        style = ttk.Style()
        if "clam" in style.theme_names():
            style.theme_use("clam")
        style.configure("Header.TLabel", font=(self._font_family, 17, "bold"))
        style.configure("Subtitle.TLabel", foreground="#6b7280")
        style.configure("Hint.TLabel", foreground="#6b7280")
        style.configure(
            "Primary.TButton", font=(self._font_family, 10, "bold"), padding=(12, 6)
        )
        style.configure("TButton", padding=(10, 5))

        self.pack(fill="both", expand=True)

        header = ttk.Frame(self)
        header.pack(fill="x", pady=(0, 10))
        ttk.Label(header, text="DocScan", style="Header.TLabel").pack(side="left")
        ttk.Label(
            header,
            text="   вырезание документа по контуру",
            style="Subtitle.TLabel",
        ).pack(side="left", pady=(6, 0))

        toolbar = ttk.Frame(self)
        toolbar.pack(fill="x", pady=(0, 12))

        self.open_button = ttk.Button(
            toolbar,
            text="Открыть фото",
            command=self.open_file,
            style="Primary.TButton",
        )
        self.open_button.pack(side="left")

        self.save_button = ttk.Button(
            toolbar, text="Сохранить результат", command=self.save_result
        )
        self.save_button.pack(side="left", padx=(8, 0))

        ttk.Separator(toolbar, orient="vertical").pack(
            side="left", fill="y", padx=14
        )
        ttk.Label(toolbar, text="Режим:").pack(side="left", padx=(0, 4))
        self.color_radio = ttk.Radiobutton(
            toolbar,
            text="Цветной",
            value="color",
            variable=self.mode,
            command=self._reprocess,
        )
        self.color_radio.pack(side="left")
        self.bw_radio = ttk.Radiobutton(
            toolbar,
            text="Ч/б скан",
            value="bw",
            variable=self.mode,
            command=self._reprocess,
        )
        self.bw_radio.pack(side="left", padx=(4, 12))

        self.shadows_check = ttk.Checkbutton(
            toolbar,
            text="Убирать тени",
            variable=self.remove_shadows_var,
            command=self._reprocess,
        )
        self.shadows_check.pack(side="left")

        self.progress = ttk.Progressbar(toolbar, mode="indeterminate", length=130)
        self.progress.pack(side="right", padx=(8, 0))

        content = ttk.Frame(self)
        content.pack(fill="both", expand=True)
        content.columnconfigure(0, weight=1, uniform="preview")
        content.columnconfigure(1, weight=1, uniform="preview")
        content.rowconfigure(0, weight=1)

        original_frame = ttk.LabelFrame(content, text=" Оригинал ", padding=8)
        original_frame.grid(row=0, column=0, sticky="nsew", padx=(0, 6))
        result_frame = ttk.LabelFrame(content, text=" Результат ", padding=8)
        result_frame.grid(row=0, column=1, sticky="nsew", padx=(6, 0))

        self.original_canvas = self._create_canvas(original_frame)
        self.result_canvas = self._create_canvas(result_frame)

        self._set_canvas_message(
            self.original_canvas,
            "Откройте фото\nили перетащите файл сюда",
        )
        self._set_canvas_message(
            self.result_canvas,
            "Здесь появится\nвырезанный документ",
        )

        ttk.Label(
            self,
            textvariable=self.status_var,
            style="Hint.TLabel",
            anchor="w",
        ).pack(fill="x", pady=(12, 0))

    def _create_canvas(self, parent: tk.Misc) -> tk.Canvas:
        canvas = tk.Canvas(
            parent,
            width=self.PREVIEW_WIDTH,
            height=self.PREVIEW_HEIGHT,
            bg="#202124",
            highlightthickness=0,
            bd=0,
        )
        canvas.pack(fill="both", expand=True)
        canvas.bind(
            "<Configure>", lambda _event, c=canvas: self._schedule_redraw(c)
        )
        return canvas

    def _setup_dnd(self) -> None:
        if DND_FILES is None or not hasattr(self.master, "drop_target_register"):
            return
        try:
            self.master.drop_target_register(DND_FILES)
            self.master.dnd_bind("<<Drop>>", self._on_drop)
        except Exception:
            pass

    def _on_drop(self, event) -> None:
        try:
            paths = self.master.tk.splitlist(event.data)
        except Exception:
            paths = [event.data]

        for raw_path in paths:
            path = str(raw_path).strip("{}")
            if path:
                self.open_file(path)
                break

    def open_file(self, path: Optional[str] = None) -> None:
        if self._busy:
            return

        if path is None:
            path = filedialog.askopenfilename(
                title="Выберите фото с документом",
                filetypes=[
                    (
                        "Изображения",
                        "*.jpg *.jpeg *.png *.bmp *.tif *.tiff *.webp",
                    ),
                    ("Все файлы", "*.*"),
                ],
            )
        if not path:
            return

        try:
            image = load_image(path)
        except Exception as exc:
            messagebox.showerror(
                "Ошибка", f"Не удалось открыть изображение:\n{path}\n\n{exc}"
            )
            return

        self.source_image = image
        self.source_path = path
        self.result = None
        self._draw_image(self.original_canvas, image)
        self._set_canvas_message(self.result_canvas, "Обработка…")
        self._enqueue_processing()

    def _reprocess(self) -> None:
        if self.source_image is None or self._busy:
            return
        self._set_canvas_message(self.result_canvas, "Обработка…")
        self._enqueue_processing()

    def _enqueue_processing(self) -> None:
        if self.source_image is None:
            return

        self.result = None
        self._job_id += 1
        job_id = self._job_id
        image = self.source_image
        bw = self.mode.get() == "bw"
        remove_shadows = bool(self.remove_shadows_var.get())

        self._set_busy(True)
        self.status_var.set("Обработка изображения…")

        result_queue: queue.Queue = queue.Queue()

        def worker() -> None:
            try:
                result = process(
                    image, bw=bw, remove_shadows=remove_shadows
                )
                result_queue.put((job_id, result, None))
            except Exception as exc:
                result_queue.put((job_id, None, exc))

        threading.Thread(target=worker, daemon=True).start()
        self.after(60, lambda: self._poll_result(result_queue))

    def _poll_result(self, result_queue: queue.Queue) -> None:
        try:
            job_id, result, error = result_queue.get_nowait()
        except queue.Empty:
            self.after(60, lambda: self._poll_result(result_queue))
            return

        if job_id != self._job_id:
            return

        self._set_busy(False)

        if error is not None:
            self._set_canvas_message(
                self.result_canvas, "Не удалось обработать изображение"
            )
            self.status_var.set("Ошибка обработки.")
            messagebox.showerror(
                "Ошибка", f"Не удалось обработать изображение:\n{error}"
            )
            self._update_controls()
            return

        self.result = result
        self._draw_image(self.result_canvas, result.image)
        if result.found:
            self.status_var.set(
                "Документ найден и выровнен. Можно сохранить результат или сменить режим."
            )
        else:
            self.status_var.set(
                "Контур не найден — показано всё фото. Попробуйте снять документ на контрастном фоне."
            )
        self._update_controls()

    def save_result(self) -> None:
        if self.result is None or self._busy:
            return

        base_name = os.path.splitext(
            os.path.basename(self.source_path or "document")
        )[0]
        initial_dir = (
            os.path.dirname(self.source_path) if self.source_path else os.getcwd()
        )

        path = filedialog.asksaveasfilename(
            title="Сохранить документ",
            defaultextension=".jpg",
            initialdir=initial_dir,
            initialfile=f"{base_name}_scan.jpg",
            filetypes=[("JPEG", "*.jpg *.jpeg"), ("PNG", "*.png")],
        )
        if not path:
            return

        try:
            save_image(path, self.result.image)
        except Exception as exc:
            messagebox.showerror(
                "Ошибка", f"Не удалось сохранить файл:\n{path}\n\n{exc}"
            )
            return

        self.status_var.set(f"Сохранено: {path}")

    def _set_busy(self, busy: bool) -> None:
        self._busy = busy
        if busy:
            self.progress.start(12)
        else:
            self.progress.stop()
        self._update_controls()

    def _update_controls(self) -> None:
        has_source = self.source_image is not None
        has_result = self.result is not None

        if self._busy:
            self.open_button.state(["disabled"])
            self.save_button.state(["disabled"])
            self.color_radio.state(["disabled"])
            self.bw_radio.state(["disabled"])
            self.shadows_check.state(["disabled"])
            return

        self.open_button.state(["!disabled"])
        self.save_button.state(["!disabled"] if has_result else ["disabled"])
        for widget in (self.color_radio, self.bw_radio, self.shadows_check):
            widget.state(["!disabled"] if has_source else ["disabled"])

    def _set_canvas_message(self, canvas: tk.Canvas, message: str) -> None:
        self._canvas_images.pop(canvas, None)
        self._photos.pop(canvas, None)
        self._canvas_messages[canvas] = message

        canvas.delete("all")
        width = max(canvas.winfo_width(), self.PREVIEW_WIDTH)
        height = max(canvas.winfo_height(), self.PREVIEW_HEIGHT)
        canvas.create_text(
            width // 2,
            height // 2,
            text=message,
            fill="#9aa0a6",
            font=(self._font_family, 12),
            justify="center",
            width=min(width - 40, 420),
        )

    def _draw_image(self, canvas: tk.Canvas, image: cv2.Mat) -> None:
        self._canvas_images[canvas] = image
        self._canvas_messages.pop(canvas, None)
        self._redraw_canvas(canvas)

    def _redraw_canvas(self, canvas: tk.Canvas) -> None:
        self._resize_jobs.pop(canvas, None)
        image = self._canvas_images.get(canvas)
        if image is None:
            return

        width = max(canvas.winfo_width(), 50)
        height = max(canvas.winfo_height(), 50)

        rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        pil_image = Image.fromarray(rgb)
        pil_image.thumbnail((width - 12, height - 12), Image.LANCZOS)

        photo = ImageTk.PhotoImage(pil_image)
        canvas.delete("all")
        canvas.create_image(width // 2, height // 2, image=photo, anchor="center")
        self._photos[canvas] = photo

    def _schedule_redraw(self, canvas: tk.Canvas) -> None:
        if canvas in self._canvas_images:
            previous_job = self._resize_jobs.pop(canvas, None)
            if previous_job is not None:
                try:
                    self.after_cancel(previous_job)
                except Exception:
                    pass
            self._resize_jobs[canvas] = self.after(
                120, lambda: self._redraw_canvas(canvas)
            )
        elif canvas in self._canvas_messages:
            previous_job = self._resize_jobs.pop(canvas, None)
            if previous_job is not None:
                try:
                    self.after_cancel(previous_job)
                except Exception:
                    pass
            message = self._canvas_messages[canvas]
            self._resize_jobs[canvas] = self.after(
                120, lambda: self._set_canvas_message(canvas, message)
            )


def _create_root() -> tk.Tk:
    if TkinterDnD is not None:
        try:
            return TkinterDnD.Tk()
        except Exception:
            pass
    return tk.Tk()


def run(initial_path: Optional[str] = None) -> None:
    root = _create_root()
    app = ScannerApp(root)

    if initial_path is None and len(sys.argv) > 1:
        candidate = sys.argv[1]
        if os.path.isfile(candidate):
            initial_path = candidate

    if initial_path:
        root.after(150, lambda: app.open_file(initial_path))

    root.mainloop()
