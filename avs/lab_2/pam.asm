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

        xor     eax, eax
        mov     ax, cs
        shl     eax, 4
        mov     [gdt_code+2], ax
        mov     [gdt_data+2], ax
        shr     eax, 16
        mov     [gdt_code+4], al
        mov     [gdt_data+4], al

        xor     eax, eax
        mov     ax, cs
        shl     eax, 4
        add     eax, gdt
        mov     [gdtr+2], eax
        mov     word [gdtr], gdt_end - gdt - 1

        mov     [save_sp], sp
        mov     [rm_ptr+2], cs

        cli
        mov     al, 80h
        out     70h, al
        lgdt    [gdtr]

        mov     eax, cr0
        or      al, 1
        mov     cr0, eax

        db      0EAh
        dw      pm_entry
        dw      CODE_SEL

pm_entry:
        mov     ax, DATA_SEL
        mov     ds, ax
        mov     ss, ax
        mov     ax, VIDEO_SEL
        mov     es, ax

        mov     si, pm_text
        mov     di, (12*80 + 20) * 2
        mov     ah, 1Eh
.print:
        lodsb
        or      al, al
        jz      .done
        stosw
        jmp     .print
.done:

        mov     ax, DATA_SEL
        mov     ds, ax
        mov     es, ax
        mov     ss, ax
        mov     fs, ax
        mov     gs, ax

        mov     eax, cr0
        and     al, 0FEh
        mov     cr0, eax

        jmp     far [cs:rm_ptr]

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
        sti

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
        dq      0
gdt_code:
        dw      0FFFFh, 0
        db      0, 9Ah, 0, 0
gdt_video:
        dw      0FFFh, 8000h
        db      0Bh, 92h, 0, 0
gdt_data:
        dw      0FFFFh, 0
        db      0, 92h, 0, 0
gdt_end:

gdtr:   dw      0
        dd      0

rm_ptr: dw      back_to_real, 0
save_sp dw      0

pm_text   db 'Hello from Protected Mode!', 0
msg_start db 'Real mode. Switching to protected mode...', 13, 10, '$'
msg_back  db 13, 10, 13, 10, 'Back in real mode. Press any key...$'
msg_v86   db 'CPU already in V86/protected mode. Run without EMM386.', 13, 10, '$'