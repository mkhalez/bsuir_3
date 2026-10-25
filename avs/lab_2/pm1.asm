        cpu     386
        bits    16
        org     100h

start:
        smsw    ax
        test    al, 1
        jz      real_ok
        mov     dx, msg_v86
        mov     ah, 9
        int     21h
        mov     ax, 4C01h
        int     21h

real_ok:
        mov     dx, msg_start
        mov     ah, 9
        int     21h

        ; подготовка: заполняем базы дескрипторов 
        xor     eax, eax
        mov     ax, cs
        shl     eax, 4                 
        mov     [gdt_code+2], ax    
        mov     [gdt_data+2], ax
        shr     eax, 16
        mov     [gdt_code+4], al       
        mov     [gdt_data+4], al

        ; GDTR предел и линейный адрес GDT 
        xor     eax, eax
        mov     ax, cs
        shl     eax, 4
        add     eax, gdt
        mov     [gdtr+2], eax
        mov     word [gdtr], gdt_end - gdt - 1

        ; запоминаем то, что нужно для возврата 
        mov     [save_sp], sp
        mov     [rm_ptr+2], cs          

        cli                             ; запрет маскируемых прерываний
        mov     al, 80h
        out     70h, al                 ; запрет NMI
        lgdt    [gdtr]                  ; загрузка GDTR

        ; CR0.PE = 1
        mov     eax, cr0
        or      al, 1
        mov     cr0, eax

        ; far jmp: очистка конвейера, загрузка CS селектором кода 
        db      0EAh
        dw      pm_entry
        dw      CODE_SEL

pm_entry:
        mov     ax, DATA_SEL
        mov     ds, ax
        mov     ss, ax
        mov     ax, VIDEO_SEL
        mov     es, ax

        mov     si, pm_textLDTR
        mov     di, (12*80 + 20) * 2    ; строка 12, столбец 20
        mov     ah, 1Eh                 ; жёлтый на синем
.print:
        lodsb
        or      al, al
        jz      .done
        stosw
        jmp     .print
.done:

        ; подготовка к возврату: реальные дескрипторы (64К, G=0, W=1, P=1) заполнение безопасным селектором
        mov     ax, DATA_SEL
        mov     ds, ax
        mov     es, ax
        mov     ss, ax
        mov     fs, ax
        mov     gs, ax

        ; CR0.PE = 0 
        mov     eax, cr0
        and     al, 0FEh
        mov     cr0, eax

        jmp     far [cs:rm_ptr]         ; far jmp: CS = реальный сегмент

back_to_real:
        mov     ax, cs
        mov     ds, ax
        mov     es, ax
        mov     ss, ax
        mov     sp, [save_sp]
        xor     ax, ax
        mov     fs, ax
        mov     gs, ax
        mov     al, 0
        out     70h, al                 
        sti                             ; разрешить прерывания

        mov     dx, msg_back
        mov     ah, 9
        int     21h
        mov     ah, 1                   
        int     21h
        mov     ax, 4C00h
        int     21h

CODE_SEL  equ 08h
VIDEO_SEL equ 10h
DATA_SEL  equ 18h

        align   8
gdt:
        dq      0                               ; 00h нулевой дескриптор
gdt_code:                                       ; 08h код: P=1 DPL=0 S=1 Code R=1
        dw      0FFFFh, 0
        db      0, 9Ah, 0, 0
gdt_video:                                      ; 10h данные: база B8000h
        dw      0FFFh, 8000h
        db      0Bh, 92h, 0, 0
gdt_data:                                       ; 18h данные: 64К, G=0, W=1, P=1
        dw      0FFFFh, 0
        db      0, 92h, 0, 0
gdt_end:

gdtr:   dw      0
        dd      0

rm_ptr: dw      back_to_real, 0                 ; смещение, сегмент 
save_sp dw      0

pm_text   db 'Hello from Protected Mode!', 0
msg_start db 'Real mode. Switching to protected mode...', 13, 10, '$'
msg_back  db 13, 10, 13, 10, 'Back in real mode. Press any key...$'
msg_v86   db 'CPU already in V86/protected mode. Run without EMM386.', 13, 10, '$'
