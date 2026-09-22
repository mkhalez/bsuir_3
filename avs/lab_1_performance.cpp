#include <iostream>
#include <cstdint>
#include <chrono>
#include <iomanip>

int main() {
    int8_t A[8] = { 5,  -3,  10, -2,   1,   4, -8,  7 };
    int8_t B[8] = { 2,   4,  -3, -5,   6,  -1,  2, -4 };
    int8_t C[8] = { 3,  -2,   4,  2,  -3,   5, -2, -3 };
    short  D[8] = { 10, 20,  -5, 15, -10,   8, 12, -6 };

    short F_cpp[8] = { 0 };
    short F_asm[8] = { 0 };

    constexpr int ITERATIONS = 10'000'000;

    auto start_cpp = std::chrono::high_resolution_clock::now();

    for (int it = 0; it < ITERATIONS; ++it) {
        for (int i = 0; i < 8; ++i) {
            F_cpp[i] = A[i] + (B[i] * C[i]) - D[i];
        }
        asm volatile("" : : "r"(F_cpp) : "memory");
    }

    auto end_cpp = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double, std::milli> duration_cpp = end_cpp - start_cpp;

    auto start_asm = std::chrono::high_resolution_clock::now();

    for (int it = 0; it < ITERATIONS; ++it) {
        asm volatile (
            ".intel_syntax noprefix\n\t"

            // =========================================================
            // ЧАСТЬ 1: Обработка элементов 0..3 (младшая четверка)
            // =========================================================

            // 1. Расширяем B[0..3] с 8 бит до 16 бит
            "movq      mm0, [%[B]]\n\t"        // mm0 = B[7]..B[0]
            "pxor      mm1, mm1\n\t"           // mm1 = 0
            "pcmpgtb   mm1, mm0\n\t"           // mm1 = знаковые байты B (0xFF для <0, иначе 0x00)
            "movq      mm2, mm0\n\t"           // Копируем B
            "punpcklbw mm2, mm1\n\t"           // mm2 = B[0..3] (16-битные знаковые)

            // 2. Расширяем C[0..3] с 8 бит до 16 бит
            "movq      mm0, [%[C]]\n\t"        // mm0 = C[7]..C[0]
            "pxor      mm1, mm1\n\t"           // mm1 = 0
            "pcmpgtb   mm1, mm0\n\t"           // mm1 = знаковые байты C
            "movq      mm3, mm0\n\t"           // Копируем C
            "punpcklbw mm3, mm1\n\t"           // mm3 = C[0..3] (16-битные знаковые)

            // 3. Умножение B[0..3] * C[0..3]
            "pmullw    mm2, mm3\n\t"           // mm2 = B[i] * C[i]

            // 4. Расширяем A[0..3] с 8 бит до 16 бит
            "movq      mm0, [%[A]]\n\t"        // mm0 = A[7]..A[0]
            "pxor      mm1, mm1\n\t"           // mm1 = 0
            "pcmpgtb   mm1, mm0\n\t"           // mm1 = знаковые байты A
            "punpcklbw mm0, mm1\n\t"           // mm0 = A[0..3] (16-битные знаковые)

            // 5. Вычисляем A[i] + (B[i] * C[i]) - D[i]
            "paddw     mm2, mm0\n\t"           // mm2 = (B * C) + A
            "movq      mm4, [%[D]]\n\t"        // mm4 = D[0..3]
            "psubw     mm2, mm4\n\t"           // mm2 = (B * C + A) - D

            // 6. Сохраняем результат для элементов 0..3
            "movq      [%[F]], mm2\n\t"

            // =========================================================
            // ЧАСТЬ 2: Обработка элементов 4..7 (старшая четверка)
            // =========================================================

            // 1. Расширяем B[4..7] до 16 бит
            "movq      mm0, [%[B]]\n\t"
            "pxor      mm1, mm1\n\t"
            "pcmpgtb   mm1, mm0\n\t"
            "punpckhbw mm0, mm1\n\t"           // mm0 = B[4..7] (16-битные знаковые)

            // 2. Расширяем C[4..7] до 16 бит
            "movq      mm2, [%[C]]\n\t"
            "pxor      mm1, mm1\n\t"
            "pcmpgtb   mm1, mm2\n\t"
            "punpckhbw mm2, mm1\n\t"           // mm2 = C[4..7] (16-битные знаковые)

            // 3. Умножение B[4..7] * C[4..7]
            "pmullw    mm0, mm2\n\t"           // mm0 = B[i] * C[i]

            // 4. Расширяем A[4..7] до 16 бит
            "movq      mm3, [%[A]]\n\t"
            "pxor      mm1, mm1\n\t"
            "pcmpgtb   mm1, mm3\n\t"
            "punpckhbw mm3, mm1\n\t"           // mm3 = A[4..7] (16-битные знаковые)

            // 5. Вычисляем A[i] + (B[i] * C[i]) - D[i]
            "paddw     mm0, mm3\n\t"           // mm0 = (B * C) + A
            "movq      mm4, [%[D] + 8]\n\t"    // mm4 = D[4..7] (смещение 8 байт)
            "psubw     mm0, mm4\n\t"           // mm0 = (B * C + A) - D

            // 6. Сохраняем результат для элементов 4..7
            "movq      [%[F] + 8], mm0\n\t"

            // Очистка состояния MMX
            "emms\n\t"
            ".att_syntax\n\t"
            :
            : [A] "r" (A), [B] "r" (B), [C] "r" (C), [D] "r" (D), [F] "r" (F_asm)
            : "mm0", "mm1", "mm2", "mm3", "mm4", "memory"
        );
    }

    auto end_asm = std::chrono::high_resolution_clock::now();
    std::chrono::duration<double, std::milli> duration_asm = end_asm - start_asm;

    std::cout << std::fixed << std::setprecision(3);
    std::cout << "===========================================\n";
    std::cout << "Бенчмарк (" << ITERATIONS << " итераций):\n";
    std::cout << "===========================================\n";
    std::cout << "Время C++ (поэлементно): " << duration_cpp.count() << " ms\n";
    std::cout << "Время MMX (ASM SIMD):    " << duration_asm.count() << " ms\n";
    if (duration_asm.count() > 0) {
        std::cout << "Ускорение (Ratio):      " << (duration_cpp.count() / duration_asm.count()) << "x\n";
    }
    std::cout << "===========================================\n\n";

    std::cout << "Сравнение результатов вычислений:\n";
    for (int i = 0; i < 8; i++) {
        std::cout << "F_cpp[" << i << "] = " << F_cpp[i] 
                  << "\t| F_asm[" << i << "] = " << F_asm[i] << "\n";
    }

    return 0;
}