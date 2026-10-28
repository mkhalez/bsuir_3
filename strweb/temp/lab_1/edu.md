# ЛР1 СТРВП (HTML) — теория для защиты + карта выполнения в проекте

Тематика проекта: прокат автомобилей. Стек: Django + HTML5.
Демо всех HTML-элементов: `GET /lr1/` → `lab_5/views.py:568 lr1_demo_view` → `lab_5/templates/lab_5/lr1_demo.html` (наследует `lab_5/templates/lab_5/base.html`).
Базовый шаблон для единообразия: `lab_5/templates/lab_5/base.html` (doctype, head с meta, header+nav, main с `{% block content %}`).

---

## 1. Структура HTML-документа

```html
<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>...</title>
</head>
<body>...</body>
</html>
```

- `<!DOCTYPE html>` — режим стандартов HTML5, без него quirks mode. У нас в `base.html:1`.
- `html/head/body` — рекомендованы стандартом; минимальный валидный документ может состоять из doctype + title + текст, но так не делать.
- `lang="ru"` — язык документа, влияет на кавычки, скринридеры, перевод.
- Комментарии `<!-- ... -->` игнорируются браузером.
- Спецсимволы: `&lt; &gt; &amp; &quot; &nbsp; &copy;` — нужны т.к. `<`, `>` зарезервированы. Вид: `&имя;` или `&#код;`.
- HTML регистронезависим для тегов/атрибутов, но значения (пути на Linux) регистрозависимы. Атрибуты: `имя="значение"`, кавычки обязательны при пробелах/кириллице, рекомендуются всегда. Логические атрибуты: достаточно имени (`required`, `controls`, `hidden`).
- Контейнеры (`<p>...</p>`) vs void-элементы (`<img>`, `<br>`, `<input>`, `<meta>`, `<source>`) — у вторых нет закрывающего тега.
- Вложенность обязана быть корректной: `<i>курсив<b>жирный</b></i>`, пересечения запрещены.
- XHTML (для справки): тот же HTML, но как правильный XML — все теги закрыты, нижний регистр, `атрибут="атрибут"` вместо логических.
- Emmet из пр-2 (могут спросить): `> + ^ * $ {} () # . [] :` — например `ul>li.item$*3` разворачивается в 3 `li`.

Что сказать на защите: «doctype включает стандартный режим, head — служебная информация, body — отображаемое содержимое».

---

## 2. Head: meta, микроразметка, favicon

Метаданные — служебная информация в `head`, не рендерится, но нужна поисковикам/сетям (лекция 1, слайды 57–60; задание ссылается на MDN «The head metadata in HTML»).

| Тег | Зачем |
|---|---|
| `<meta charset="UTF-8">` | кодировка |
| `<meta name="viewport" content="width=device-width, initial-scale=1.0">` | адаптивность на мобильных |
| `<meta name="description" content="...">` | сниппет в поисковой выдаче |
| `<meta name="keywords" content="...">` | ключевые слова (исторически) |
| `<meta name="author" content="...">` | автор |
| `<meta property="og:title/description/type" ...>` + `<meta name="twitter:card">` + `<meta name="theme-color">` | Open Graph / Twitter Cards / цвет вкладки |
| `<link rel="icon" href="..." type="image/svg+xml">` | иконка вкладки (favicon) |

Микроданные (microdata, слайды 52–54 пр-1) — машиночитаемая семантика для поисковиков через глобальные атрибуты:

```html
<div itemscope itemtype="https://schema.org/CarRental">
  <meta itemprop="name" content="Прокат авто — ЛР1 демо">
  <span itemprop="genre">Фантастика</span>
</div>
```

- `itemscope` — начало сущности, `itemtype` — тип из словаря schema.org, `itemprop` — свойство («ключ»: `summary`, `photo`, `trailer` и т.д.).
- Плюс `data-*` атрибуты (`<li data-animaltype="bird">`, `<div data-page="lr1-demo">`) — пользовательские данные для JS, имя без заглавных букв.

Где в проекте: `base.html:4-9` (charset, viewport, description, keywords, author, favicon-SVG через data-URI), `lr1_demo.html:5-19` (OG/Twitter/theme-color + `itemscope itemtype="https://schema.org/CarRental"`).

---

## 3. Семантическая верстка

Семантика = тег описывает смысл, а не внешний вид (лекция 1; ссылки задания: MDN «Document and website structure», w3schools `html5_semantic_elements`).

```html
<header>...</header>
<nav aria-label="...">ссылки</nav>
<main>
  <section id="text"><h2>...</h2><article>...</article></section>
  <aside>боковая панель</aside>
</main>
<footer>...</footer>
```

- `header` — шапка, `nav` — навигация (у нас их 2+: горизонтальная с якорями + вертикальная в `aside`, требование «2 nav» закрыто в `lr1_demo.html:25,40` + меню сайта в `base.html:15`), `main` — уникальное содержимое (одно на страницу), `section` — тематический раздел с заголовком, `article` — автономная единица (новость/отзыв), `aside` — побочное, `footer` — подвал, `address` — контакты.
- `aria-label`, `aria-labelledby` — доступность для скринридеров.
- Проверка семантики на защите: показать исходник `/lr1/`, назвать теги; валидатор `validator.w3.org/nu`.

---

## 4. Текст, глобальные атрибуты, листинги, цитаты, время, стих

Семантическое выделение (вместо устаревших `<b>/<i>` для смысла):

- `<strong>` — важность, `<em>` — акцент, `<mark>` — подсветка, `<ins>`/`<del>` — вставлено/удалено, `<small>` — мелкий текст, `<sub>`/`<sup>` — под/надстрочный, `<code>` — код, `<span>` — нейтральный строчный контейнер.
- `<abbr title="...">` — аббревиатура, `<dfn>` — первое определение термина.
- Цитаты: `<blockquote>` — блочная (сертификат в `about.html:41`), `<q>` — строчная, `<cite>` — источник.
- `<time datetime="2024-03-14T19:00">14 марта</time>` — машиночитаемые дата/время; у нас даты FAQ/новостей/отзывов (`faq.html:10-12`, `news.html`, `reviews.html`).
- Стих с фиксированным и «мягким» переносом: `<pre>` сохраняет пробелы/переводы, `<br>` — жёсткий перевод строки, `<wbr>` — потенциальное место переноса длинного слова. Пример из демо — четверостишие в `<pre>` с `<br>` и `<wbr>`.
- Листинг кода: `<pre><code>...</code></pre>` (экранировать `<` как `&lt;`).

Глобальные атрибуты (работают на любом теге, слайды 37–52 пр-1):

- `id` (уникален, регистрозависим), `class` (группы через пробел), `style` (inline CSS), `title` (tooltip), `accesskey` (Alt+клавиша, у нас `accesskey="1"`), `tabindex`, `dir="ltr|rtl|auto"`, `lang`, `hidden` (не отображать, напр. `<foo hidden>`), `contenteditable`, `spellcheck`, `translate="yes|no"`, `data-*`, `draggable`, события `onclick` и т.д.
- В демо: `id="page-title" class="demo-title" title="..." data-page="..." accesskey="1"`.

---

## 5. Ссылки, якоря, контейнеры

```html
<a href="https://example.com">внешняя</a>
<a href="/news/1/">внутренняя (Django {% url %})</a>
<a href="#table">якорь вниз</a>
<h2 id="table">...</h2>
<a href="#top">наверх</a>
<a href="/media/contract.pdf" download>скачать договор (атрибут download)</a>
<a href="mailto:info@prokat.by">почта</a>
```

- Контейнеры: `div` — блочный, `p` — абзац, `pre` — преформат, `hr` — разделитель, `blockquote` — цитата. Все есть в `lr1_demo.html`.
- Якоря: `href="#id"` + `id="..."` на цели; в демо 7 якорей (`#text #code #links #lists #table #form #media`) + `#top`.

---

## 6. Списки, фигуры, таблица

Списки:

```html
<ul><li>маркированный</li></ul>
<ol><li>нумерованный</li></ol>
<dl><dt>термин</dt><dd>определение</dd></dl>
```

Фигуры и адаптивные картинки:

```html
<figure>
  <img src="low.jpg" alt="Обязательный alt" width="300"
       srcset="low.jpg 600w, high.jpg 1000w"
       sizes="(max-width: 40em) 100vw, 50vw">
  <figcaption>Подпись</figcaption>
</figure>
<picture>
  <source media="(max-width: 20em)" srcset="small.jpg 1x, small2x.jpg 2x">
  <img src="large.jpg" alt="...">
</picture>
```

- `alt` обязателен (читалки, отключённые картинки, валидатор ругается). Проверка проекта: 0 `img` без `alt`.
- `srcset` с `x` (плотность: `1x 2x 3x`) или `w` (ширина: `600w`) + `sizes` (какую ширину займёт картинка: `(max-width: 40em) 100vw, 50vw`). `picture/source media/type` — арт-дирекшн под разные экраны/форматы (слайды 74–90 пр-3).

Таблица только для данных (не для вёрстки!), с заголовочными ячейками и `headers`:

```html
<table>
  <caption>Тарифы</caption>
  <thead><tr><th id="c1">Тариф</th><th id="c2">Сутки</th></tr></thead>
  <tbody>
    <tr><td headers="c1">Эконом</td><td headers="c2">50</td></tr>
    <tr><td headers="c1" colspan="2">объединение по горизонтали</td></tr>
    <tr><td headers="c1" rowspan="2">...</td><td headers="c2">...</td></tr>
  </tbody>
</table>
```

- `th` — заголовочная, `colspan/rowspan` — объединение, `headers="id"` — явная связь ячейки с заголовком для скринридеров. В демо таблица тарифов содержит всё это + цифровые и текстовые данные.

---

## 7. Формы и валидация (лекция 3, слайды 3–64)

```html
<form action="/reviews/" method="post">
  <fieldset title="...">
    <legend>Группа полей</legend>
    <label for="usr">Имя:</label>
    <input id="usr" type="text" name="username" required minlength="3" placeholder="Иван">
    <label>Пароль:<input type="password" name="pass" required></label>
    <input type="email" name="email" required>
    <input type="number" name="days" min="1" max="30" step="1">
    <input type="tel" name="phone" pattern="\+375[0-9]{9}">
    <input type="checkbox" name="v" value="Bike" checked>
    <input type="radio" name="gender" value="male">
    <input type="date" name="d"> <input type="color" name="c">
    <input type="range" name="r" min="10" max="20">
    <input type="file" name="f">
    <input type="hidden" name="car_id" value="1">
    <select name="s"><option value="1">Эконом</option><optgroup label="Группа">...</optgroup></select>
    <textarea name="text" rows="4" cols="45" maxlength="450" required></textarea>
    <input list="dl"><datalist id="dl"><option value="Apples"></datalist>
    <button type="submit">Отправить</button>
    <input type="reset" value="Очистить">
  </fieldset>
</form>
```

Теория, которую спрашивают:

- `form`: `action` (URL обработчика; без него — перезагрузка текущей), `method` (`get` — параметры в URL, видны, ограничены длиной; `post` — в теле запроса), `enctype="multipart/form-data"` обязателен для `type="file"`, `novalidate` — отключить встроенную проверку, `autocomplete`, `target`, `name`.
- `input type`: `text password email number range color date datetime-local month week time tel url search checkbox radio button submit reset image file hidden`.
- Атрибуты: `name` (ключ `name=value` при отправке; у radio группы `name` одинаковый), `value`, `required`, `pattern` (регулярка), `min/max/step`, `minlength/maxlength/size`, `placeholder`, `readonly` (отправляется) vs `disabled` (не отправляется, нет tab), `checked/selected`, `autofocus`, `form` (поле вне формы), `formaction/formmethod/...` у кнопок.
- `textarea` (`rows/cols/maxlength/placeholder/required`), `select+option+optgroup` (`multiple/size/disabled/selected/label/value`), `datalist+input[list]` (подсказки), `button type="button|submit|reset"` (внутри можно разметку, в отличие от `input`), `fieldset+legend` (группировка), `label` двумя способами: `for="id"` или обёртка.
- `type="image"`: как submit + шлёт координаты `img.x/img.y`. Checkbox/radio шлют `name=value`, а не true/false.
- Django: в шаблонах форм обязателен `{% csrf_token %}`; серверная валидация — в `forms.py` / `ModelForm`.

Где в проекте: демо-форма в `lr1_demo.html` (все виды input, select, textarea, валидация), реальные формы — отзыв (`reviews.html` + `ReviewForm`), корзина (`car_detail.html`, `cart.html`), оплата (`checkout.html` с `pattern` для карты), вход/регистрация/профиль.

---

## 8. Iframe, embed/object, video/audio (лекция 3, слайды 91–117)

```html
<a href="https://ru.wikipedia.org/wiki/HTML" target="fr">HTML</a>
<iframe name="fr" src="..." width="400" height="150" sandbox="allow-scripts">Текст если нет поддержки</iframe>
<video controls width="480" preload="none" poster="p.jpg">
  <source src="film.webm" type="video/webm">
  <source src="film.mp4" type="video/mp4">
  <track kind="subtitles" src="sub_ru.vtt" srclang="ru" label="Русский" default>
  Нет поддержки video.
</video>
<audio controls>
  <source src="track.ogg" type="audio/ogg">
  <source src="track.mp3" type="audio/mpeg">
</audio>
```

- `iframe`: встроенный документ; `src` — что грузить, `srcdoc` — HTML строкой, `name/target` — загрузка ссылки во фрейм, `sandbox` — песочница (без значения режет скрипты/формы/доступ к родителю; `allow-scripts allow-forms allow-same-origin allow-top-navigation` — точечно разрешить). Старые `frameset/frame` — устарели.
- `embed` (void, `src/type/width/height`) и `object+param` (контейнер с fallback) — наследие плагинов (Flash); идея одна — чужой контент.
- `video/audio`: нативные, без плагинов; атрибуты `controls autoplay loop muted preload="none|metadata|auto" poster width/height src`; несколько `source` — кросс-браузерность кодеков; `track kind="subtitles|captions"` — субтитры; вложенный текст — fallback. В проекте: `about.html:15-27` (`video` из `CompanyInfo.video` или `video_url`), демо `video+audio+iframe` в `lr1_demo.html`.

---

## 9. Django-карта: как каждая строка задания закрыта кодом

| Требование | Модель (таблица БД) | View | URL | Шаблон |
|---|---|---|---|---|
| Главная: логотип, баннер (несколько картинок), каталог, последняя статья из БД, партнёры с логотипами+ссылками | `News`, `Car`, `Partner` (`models.py:263`) | `home_view` (`views.py:30`) | `/` | `home.html` (баннер 3 картинки, каталог 6 авто со ссылкой в `car_detail`, последняя статья + «Читать далее», партнёры) |
| Страница товара + «Добавить в корзину» | `Car` | `car_detail_view` (`views.py:471`) | `cars/<id>/` | `car_detail.html` (форма days 1–30, POST) |
| Корзина: список, «Оплатить», «Удалить», «Увеличить/уменьшить» | сессия `request.session['cart']` (`_get_cart/_save_cart`) | `cart_view/cart_add/cart_update/cart_remove` (`views.py:477-531`) | `cart/`, `cart/add|update|remove` | `cart.html` |
| Оплата | при POST залогиненного создаются `Rental` | `checkout_view` (`views.py:534`) | `checkout/` | `checkout.html` (демо-карта с `pattern`) + `payment_success.html` |
| О компании: инфо, видео, логотип, история по годам, реквизиты, сертификат | `CompanyInfo` (`models.py:149`: `requisites certificate_text video video_url logo history_years`) | `about_view` (`views.py:46`) | `about/` | `about.html` (логотип `img alt`, `video`, реквизиты, `blockquote` сертификат) |
| Новости: заголовок + 1 предложение + картинка + «Читать далее» → вся статья | `News` (`models.py:168`: `short_description content image`) | `news_view`, `news_detail_view` (`views.py:442-450`) | `news/`, `news/<id>/` | `news.html` (кратко), `news_detail.html` (полностью) |
| Словарь/FAQ: список с датой, клик → ответ | `FAQ` (`models.py:184`: `created_at`) | `faq_view` (`views.py:430`) | `faq/` | `faq.html` (`details/summary`, дата) |
| Контакты: фото, должность, телефоны, почта | `Employee` (`models.py:198`: `photo position phone email`) | `contacts_view` (`views.py:416`) | `contacts/` | `contacts.html` (`last_name+first_name`, alt починен) |
| Политика конфиденциальности | — (статика) | `privacy_view` | `privacy/` | `privacy.html` |
| Вакансии | `Vacancy` | `vacancies_view` | `vacancies/` | `vacancies.html` |
| Отзывы: имя, оценка, текст, дата + форма (залогиненные; аноним → login) | `Review` (`models.py:230`) | `reviews_view` (`views.py:392`; POST сохраняет, `user_name=request.user.username`) | `reviews/` | `reviews.html` |
| Промокоды действующие + архив | `PromoCode.is_archived` (`models.py:248`) | `promocodes_view` (`views.py:453`) | `promocodes/` | `promocodes.html` (2 секции) |
| HTML-элементы (п. «Использовать») | — | `lr1_demo_view` | `lr1/` | `lr1_demo.html` + `base.html` |
| Базовый шаблон | — | — | — | `base.html` (меню: Корзина, Промокоды, Конфиденциальность и др.) |

Админка: `Partner` зарегистрирован в `lab_5/admin.py`; `CompanyInfo/News/FAQ/Employee/Vacancy/Review/PromoCode` тоже через админку наполняются.

---

## 10. Что показать на защите (чек-лист)

1. Открыть `/` — логотип, баннер, каталог (клик → `cars/<id>/` → «Добавить в корзину»), последняя статья, партнёры со ссылками.
2. `cars/1/` → добавить 3 суток → 302 → `/cart/` (Увеличить/Уменьшить/Удалить/Оплатить) → `/checkout/` → оплата → `payment_success.html`, корзина пуста; у залогиненного созданы `Rental`.
3. `/news/` — только кратко + «Читать далее» → `/news/1/` полный текст. `/faq/` — даты + клик по `summary` раскрывает `details`. `/about/` — логотип, video, реквизиты, сертификат в `blockquote`.
4. `/lr1/` — по очереди показать: meta/OG в исходнике, favicon во вкладке, `header/nav/main/section/article/aside`, `strong/em/mark/abbr/dfn/blockquote/time/pre/br/wbr`, ссылки + `download`, якоря `#...`, `ul/ol/dl`, `figure/picture/srcset`, 2 `nav`, таблицу с `th/colspan/rowspan/headers`, форму с `required/pattern/min/max`, `iframe`, `img alt`, `video/audio`.
5. Валидация: `manage.py check` (0 ошибок); все GET `/ /about/ /cars/ /news/ /faq/ /contacts/ /vacancies/ /reviews/ /privacy/ /lr1/ /cart/ /checkout/ /promocodes/ /cars/1/ /news/1/` → 200; вставить URL `/lr1/` и `/` в `https://validator.w3.org/nu/`; `img` без `alt` — 0; doctype на месте.
6. Важно: перед защитой наполнить БД через админку — `Partner` 2–3 шт (логотип + URL), `CompanyInfo` (логотип, реквизиты, сертификат, `video_url`), `News` с картинками, `PromoCode` активный + архивный. Сейчас в dev-БД: `CompanyInfo 0, Partner 0` — поэтому `/about/` показывает заглушку «пока не заполнена», а партнёров на главной нет; это не баг кода, а пустые таблицы (шаблоны это обрабатывают через `{% if %}`).

---

## 11. Типовые вопросы и короткие ответы

- *GET vs POST?* GET — параметры в URL, видны, лимит длины; POST — в теле. Формы с данными/оплатой — POST + CSRF.
- *Зачем `headers` в таблице?* Связь ячейки с `th` для скринридеров; таблицы только для данных, не для вёрстки.
- *`srcset/sizes/picture`?* Выбор картинки под плотность/ширину экрана; `w` — ширина файла, `x` — плотность, `sizes` — слот в вёрстке, `picture` — разные кадры/форматы по `media/type`.
- *`sandbox` у iframe?* Песочница; пустой режет всё, `allow-*` разрешает точечно.
- *`required/pattern/novalidate`?* Встроенная валидация браузера; `novalidate` её отключает.
- *`readonly` vs `disabled`?* Первое отправляется, второе нет (и нет фокуса/tab).
- *Микроданные?* `itemscope/itemtype/itemprop` — сущность/тип/свойство для поисковиков.
- *Где корзина хранится?* В сессии (`request.session['cart'] = {car_id: days}`), в БД пишется только `Rental` после оплаты залогиненным.
- *Что за `details/summary`?* Нативный раскрывающийся блок для FAQ.
- *Как обеспечен единый стиль?* Наследование `base.html` (`{% extends %}` + `{% block content %}`).
