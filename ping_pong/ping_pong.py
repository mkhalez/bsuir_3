import math
import random
import time
import tkinter as tk


WIDTH = 960
HEIGHT = 600
FPS_MS = 16
WIN_SCORE = 7

PADDLE_W = 14
PADDLE_H = 104
PADDLE_MARGIN = 34
PADDLE_SPEED = 520
AI_SPEED = 390

BALL_RADIUS = 11
BALL_START_SPEED = 430
BALL_MAX_SPEED = 1050

BG_TOP = (7, 17, 31)
BG_BOTTOM = (10, 27, 48)
BG = "#07111f"
GRID = "#0d2136"
LINE = "#163a57"
TEXT = "#dff7ff"
MUTED = "#5c829d"
CYAN = "#38d9ff"
CYAN_DIM = "#0f4d75"
PINK = "#ff4f9a"
PINK_DIM = "#6b2147"
WHITE = "#ffffff"


def mix(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def color(rgb):
    return "#%02x%02x%02x" % rgb


class Pong:
    def __init__(self, root):
        self.root = root
        self.root.title("Neon Pong")
        self.root.configure(bg=BG)
        self.root.resizable(False, False)

        self.canvas = tk.Canvas(
            root,
            width=WIDTH,
            height=HEIGHT,
            bg=BG,
            highlightthickness=0,
        )
        self.canvas.pack()
        self.canvas.focus_set()

        self.keys = set()
        self.paused = False
        self.game_over = False
        self.winner = None
        self.left_score = 0
        self.right_score = 0
        self.left_y = HEIGHT / 2
        self.right_y = HEIGHT / 2
        self.ball_x = WIDTH / 2
        self.ball_y = HEIGHT / 2
        self.ball_vx = 0.0
        self.ball_vy = 0.0
        self.serve_timer = 0.0
        self.serve_direction = 1
        self.trail = []
        self.last_time = time.perf_counter()
        self.running = True

        self.draw_background()
        self.reset_match()

        root.bind("<KeyPress>", self.on_key_press)
        root.bind("<KeyRelease>", self.on_key_release)
        root.protocol("WM_DELETE_WINDOW", self.close)
        root.after(50, root.focus_force)
        root.after(FPS_MS, self.loop)

    def draw_background(self):
        bands = 48
        for i in range(bands):
            t = i / (bands - 1)
            y0 = HEIGHT * i / bands
            y1 = HEIGHT * (i + 1) / bands
            self.canvas.create_rectangle(
                0,
                y0,
                WIDTH,
                y1,
                fill=color(mix(BG_TOP, BG_BOTTOM, t)),
                outline="",
            )

        for x in range(0, WIDTH + 1, 48):
            self.canvas.create_line(x, 0, x, HEIGHT, fill=GRID)
        for y in range(0, HEIGHT + 1, 48):
            self.canvas.create_line(0, y, WIDTH, y, fill=GRID)

        for y in range(20, HEIGHT - 20, 30):
            self.canvas.create_line(
                WIDTH / 2,
                y,
                WIDTH / 2,
                y + 16,
                fill=LINE,
                width=3,
            )

        self.canvas.create_rectangle(
            10, 10, WIDTH - 10, HEIGHT - 10, outline=LINE, width=2
        )

        self.canvas.create_text(
            WIDTH / 2,
            HEIGHT - 30,
            text="\u2191 / \u2193  движение      SPACE  пауза      R  заново      ESC  выход",
            fill=MUTED,
            font=("DejaVu Sans", 11),
        )

    def reset_match(self):
        self.left_score = 0
        self.right_score = 0
        self.left_y = HEIGHT / 2
        self.right_y = HEIGHT / 2
        self.game_over = False
        self.winner = None
        self.paused = False
        self.serve_direction = random.choice((-1, 1))
        self.reset_ball(0.7)

    def reset_ball(self, delay=0.8):
        self.ball_x = WIDTH / 2
        self.ball_y = HEIGHT / 2
        self.ball_vx = 0.0
        self.ball_vy = 0.0
        self.serve_timer = delay
        self.trail = []

    def launch_ball(self):
        speed = min(BALL_START_SPEED + (self.left_score + self.right_score) * 8, 620)
        angle = random.uniform(-0.55, 0.55)
        self.ball_vx = math.cos(angle) * speed * self.serve_direction
        self.ball_vy = math.sin(angle) * speed

    def on_key_press(self, event):
        key = event.keysym
        if key in ("Up", "Down", "w", "W", "s", "S"):
            self.keys.add(key)
        elif key == "space":
            if self.game_over:
                self.reset_match()
            else:
                self.paused = not self.paused
        elif key in ("r", "R"):
            self.reset_match()
        elif key == "Return" and self.game_over:
            self.reset_match()
        elif key == "Escape":
            self.close()

    def on_key_release(self, event):
        self.keys.discard(event.keysym)

    def close(self):
        self.running = False
        self.root.destroy()

    def loop(self):
        if not self.running:
            return
        now = time.perf_counter()
        dt = min(now - self.last_time, 0.05)
        self.last_time = now
        self.update(dt)
        self.draw()
        self.root.after(FPS_MS, self.loop)

    def update(self, dt):
        if self.paused or self.game_over:
            return

        direction = 0
        if "Up" in self.keys or "w" in self.keys or "W" in self.keys:
            direction -= 1
        if "Down" in self.keys or "s" in self.keys or "S" in self.keys:
            direction += 1
        self.left_y += direction * PADDLE_SPEED * dt
        self.left_y = max(
            PADDLE_H / 2 + 14, min(HEIGHT - PADDLE_H / 2 - 14, self.left_y)
        )

        self.update_ai(dt)

        if self.serve_timer > 0:
            self.serve_timer -= dt
            if self.serve_timer <= 0:
                self.launch_ball()
            return

        self.update_ball(dt)

    def update_ai(self, dt):
        if self.ball_vx > 0:
            travel = max(0.0, (WIDTH - PADDLE_MARGIN - PADDLE_W - self.ball_x))
            time_to_reach = travel / max(abs(self.ball_vx), 1)
            predicted = self.ball_y + self.ball_vy * time_to_reach
            min_y = 14 + BALL_RADIUS + PADDLE_H / 2
            max_y = HEIGHT - 14 - BALL_RADIUS - PADDLE_H / 2
            span = max_y - min_y
            offset = (predicted - min_y) % (2 * span)
            if offset > span:
                offset = 2 * span - offset
            target = min_y + offset
        else:
            target = HEIGHT / 2

        delta = target - self.right_y
        max_step = AI_SPEED * dt
        self.right_y += max(-max_step, min(max_step, delta))
        self.right_y = max(
            PADDLE_H / 2 + 14, min(HEIGHT - PADDLE_H / 2 - 14, self.right_y)
        )

    def update_ball(self, dt):
        speed = math.hypot(self.ball_vx, self.ball_vy)
        steps = max(1, math.ceil(speed * dt / 7))
        step_dt = dt / steps

        for _ in range(steps):
            self.ball_x += self.ball_vx * step_dt
            self.ball_y += self.ball_vy * step_dt

            if self.ball_y - BALL_RADIUS <= 14:
                self.ball_y = 14 + BALL_RADIUS
                self.ball_vy = abs(self.ball_vy)
            elif self.ball_y + BALL_RADIUS >= HEIGHT - 14:
                self.ball_y = HEIGHT - 14 - BALL_RADIUS
                self.ball_vy = -abs(self.ball_vy)

            left_x = PADDLE_MARGIN + PADDLE_W
            right_x = WIDTH - PADDLE_MARGIN - PADDLE_W

            if (
                self.ball_vx < 0
                and self.ball_x - BALL_RADIUS <= left_x
                and self.ball_x + BALL_RADIUS >= PADDLE_MARGIN
                and abs(self.ball_y - self.left_y) <= PADDLE_H / 2 + BALL_RADIUS
            ):
                self.ball_x = left_x + BALL_RADIUS
                self.bounce(self.left_y, 1)

            if (
                self.ball_vx > 0
                and self.ball_x + BALL_RADIUS >= right_x
                and self.ball_x - BALL_RADIUS <= right_x + PADDLE_W
                and abs(self.ball_y - self.right_y) <= PADDLE_H / 2 + BALL_RADIUS
            ):
                self.ball_x = right_x - BALL_RADIUS
                self.bounce(self.right_y, -1)

            if self.ball_x < -40:
                self.score_point("right")
                return
            if self.ball_x > WIDTH + 40:
                self.score_point("left")
                return

        self.trail.append((self.ball_x, self.ball_y))
        if len(self.trail) > 14:
            self.trail.pop(0)

    def bounce(self, paddle_y, direction):
        hit = (self.ball_y - paddle_y) / (PADDLE_H / 2)
        hit = max(-1.0, min(1.0, hit))
        angle = hit * math.radians(62)
        speed = min(math.hypot(self.ball_vx, self.ball_vy) * 1.045, BALL_MAX_SPEED)
        self.ball_vx = math.cos(angle) * speed * direction
        self.ball_vy = math.sin(angle) * speed
        self.trail = []

    def score_point(self, side):
        if side == "left":
            self.left_score += 1
            self.serve_direction = 1
        else:
            self.right_score += 1
            self.serve_direction = -1

        if self.left_score >= WIN_SCORE:
            self.game_over = True
            self.winner = "left"
        elif self.right_score >= WIN_SCORE:
            self.game_over = True
            self.winner = "right"
        else:
            self.reset_ball(0.9)

    def draw(self):
        self.canvas.delete("dyn")

        self.draw_score()

        if self.serve_timer <= 0 and not self.game_over:
            self.draw_trail()
        self.draw_paddle(self.left_y, PADDLE_MARGIN, CYAN, CYAN_DIM)
        self.draw_paddle(
            self.right_y, WIDTH - PADDLE_MARGIN - PADDLE_W, PINK, PINK_DIM
        )

        if not self.game_over:
            self.draw_ball()

        if self.paused and not self.game_over:
            self.draw_overlay("ПАУЗА", "Нажмите SPACE, чтобы продолжить", CYAN)
        if self.game_over:
            title = "ПОБЕДА!" if self.winner == "left" else "ПОРАЖЕНИЕ"
            accent = CYAN if self.winner == "left" else PINK
            self.draw_overlay(
                title,
                "Нажмите R или ENTER, чтобы сыграть ещё",
                accent,
            )

    def draw_score(self):
        self.canvas.create_text(
            WIDTH / 2 - 70,
            58,
            text=str(self.left_score),
            fill=TEXT,
            font=("DejaVu Sans Mono", 46, "bold"),
            tags="dyn",
        )
        self.canvas.create_text(
            WIDTH / 2 + 70,
            58,
            text=str(self.right_score),
            fill=TEXT,
            font=("DejaVu Sans Mono", 46, "bold"),
            tags="dyn",
        )
        self.canvas.create_text(
            WIDTH / 2 - 70,
            92,
            text="ВЫ",
            fill=CYAN,
            font=("DejaVu Sans", 10, "bold"),
            tags="dyn",
        )
        self.canvas.create_text(
            WIDTH / 2 + 70,
            92,
            text="ИИ",
            fill=PINK,
            font=("DejaVu Sans", 10, "bold"),
            tags="dyn",
        )
        self.canvas.create_text(
            WIDTH / 2,
            58,
            text=":",
            fill=MUTED,
            font=("DejaVu Sans Mono", 40, "bold"),
            tags="dyn",
        )

    def draw_paddle(self, center_y, x, main, glow):
        top = center_y - PADDLE_H / 2
        bottom = center_y + PADDLE_H / 2
        self.canvas.create_rectangle(
            x - 5,
            top - 5,
            x + PADDLE_W + 5,
            bottom + 5,
            fill=glow,
            outline="",
            tags="dyn",
        )
        self.canvas.create_rectangle(
            x - 2,
            top - 2,
            x + PADDLE_W + 2,
            bottom + 2,
            fill=glow,
            outline="",
            tags="dyn",
        )
        self.canvas.create_rectangle(
            x,
            top,
            x + PADDLE_W,
            bottom,
            fill=main,
            outline="",
            tags="dyn",
        )
        self.canvas.create_rectangle(
            x + PADDLE_W / 2 - 1,
            top + 6,
            x + PADDLE_W / 2 + 1,
            bottom - 6,
            fill=WHITE,
            outline="",
            tags="dyn",
        )

    def draw_ball(self):
        if self.serve_timer > 0 and int(self.serve_timer * 6) % 2 == 0:
            return
        x, y = self.ball_x, self.ball_y
        self.canvas.create_oval(
            x - BALL_RADIUS - 12,
            y - BALL_RADIUS - 12,
            x + BALL_RADIUS + 12,
            y + BALL_RADIUS + 12,
            fill="#0a2c44",
            outline="",
            tags="dyn",
        )
        self.canvas.create_oval(
            x - BALL_RADIUS - 5,
            y - BALL_RADIUS - 5,
            x + BALL_RADIUS + 5,
            y + BALL_RADIUS + 5,
            fill=CYAN_DIM,
            outline="",
            tags="dyn",
        )
        self.canvas.create_oval(
            x - BALL_RADIUS,
            y - BALL_RADIUS,
            x + BALL_RADIUS,
            y + BALL_RADIUS,
            fill=WHITE,
            outline=CYAN,
            width=2,
            tags="dyn",
        )

    def draw_trail(self):
        colors = [
            "#0b2436",
            "#0d2d43",
            "#103850",
            "#123f5a",
            "#154965",
            "#185471",
            "#1b607e",
            "#206d8d",
            "#287c9d",
            "#318caf",
            "#3b9dc2",
            "#46aed3",
            "#52bee3",
            "#5ecdf1",
        ]
        total = len(self.trail)
        for i, (x, y) in enumerate(self.trail):
            ratio = (i + 1) / total
            r = max(1.5, BALL_RADIUS * ratio * 0.8)
            self.canvas.create_oval(
                x - r,
                y - r,
                x + r,
                y + r,
                fill=colors[min(i, len(colors) - 1)],
                outline="",
                tags="dyn",
            )

    def draw_overlay(self, title, subtitle, accent):
        self.canvas.create_rectangle(
            0,
            0,
            WIDTH,
            HEIGHT,
            fill="#020810",
            stipple="gray50",
            outline="",
            tags="dyn",
        )
        self.canvas.create_text(
            WIDTH / 2,
            HEIGHT / 2 - 34,
            text=title,
            fill=accent,
            font=("DejaVu Sans", 46, "bold"),
            tags="dyn",
        )
        self.canvas.create_text(
            WIDTH / 2,
            HEIGHT / 2 + 28,
            text=subtitle,
            fill=TEXT,
            font=("DejaVu Sans", 15),
            tags="dyn",
        )


def main():
    root = tk.Tk()
    Pong(root)
    root.update_idletasks()
    w = WIDTH
    h = HEIGHT
    x = max(0, (root.winfo_screenwidth() - w) // 2)
    y = max(0, (root.winfo_screenheight() - h) // 2 - 20)
    root.geometry(f"{w}x{h}+{x}+{y}")
    root.mainloop()


if __name__ == "__main__":
    main()
