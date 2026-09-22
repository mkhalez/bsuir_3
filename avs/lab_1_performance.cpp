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

            "movq      mm0, [%[B]]\n\t"      
            "pxor      mm1, mm1\n\t"           
            "pcmpgtb   mm1, mm0\n\t"         
            "movq      mm2, mm0\n\t"        
            "punpcklbw mm2, mm1\n\t"          

            "movq      mm0, [%[C]]\n\t"       
            "pxor      mm1, mm1\n\t"           
            "pcmpgtb   mm1, mm0\n\t"           
            "movq      mm3, mm0\n\t"           
            "punpcklbw mm3, mm1\n\t"          

        
            "pmullw    mm2, mm3\n\t"          

            "movq      mm0, [%[A]]\n\t"        
            "pxor      mm1, mm1\n\t"          
            "pcmpgtb   mm1, mm0\n\t"          
            "punpcklbw mm0, mm1\n\t"   

            "paddw     mm2, mm0\n\t"         
            "movq      mm4, [%[D]]\n\t"       
            "psubw     mm2, mm4\n\t"       

            "movq      [%[F]], mm2\n\t"

            "movq      mm0, [%[B]]\n\t"
            "pxor      mm1, mm1\n\t"
            "pcmpgtb   mm1, mm0\n\t"
            "punpckhbw mm0, mm1\n\t"         

            "movq      mm2, [%[C]]\n\t"
            "pxor      mm1, mm1\n\t"
            "pcmpgtb   mm1, mm2\n\t"
            "punpckhbw mm2, mm1\n\t"      

            "pmullw    mm0, mm2\n\t"         

            "movq      mm3, [%[A]]\n\t"
            "pxor      mm1, mm1\n\t"
            "pcmpgtb   mm1, mm3\n\t"
            "punpckhbw mm3, mm1\n\t"          

            "paddw     mm0, mm3\n\t"      
            "movq      mm4, [%[D] + 8]\n\t"   
            "psubw     mm0, mm4\n\t"          

            "movq      [%[F] + 8], mm0\n\t"

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