from django.shortcuts import render
from .models import CompanyInfo, News, Car, CarType
from django.contrib.auth import login, logout, authenticate
from django.contrib.auth.forms import UserCreationForm, AuthenticationForm
from django.shortcuts import render, redirect
from django.contrib.auth.decorators import login_required
from .forms import CarRentalForm, RegistrationForm
from .models import Rental, PromoCode, Client
import datetime
from django.http import HttpResponse
from django.shortcuts import get_object_or_404
import statistics
import calendar
from django.utils import timezone
from .models import Review, Employee, Vacancy, FAQ
from .forms import ReviewForm
import requests
import logging
import io
import base64
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
import matplotlib
matplotlib.use('Agg') 
import matplotlib.pyplot as plt
from django.contrib.admin.views.decorators import staff_member_required
from django.contrib.auth.decorators import user_passes_test
# create_rental
logger = logging.getLogger(__name__)

def home_view(request):
    """Контроллер главной страницы"""
    logger.debug("Запрос главной страницы")
    latest_news = News.objects.order_by('-published_at').first()
    
    context = {
        'latest_news': latest_news
    }
    return render(request, 'lab_5/home.html', context)


def about_view(request):
    """Контроллер страницы 'О компании'"""
    logger.debug("Запрос страницы 'О компании'")
    company_info = CompanyInfo.objects.first()
    
    context = {
        'company_info': company_info
    }
    return render(request, 'lab_5/about.html', context)

def cars_list_view(request):
    """Контроллер со списком автомобилей + интеграция 2 сторонних API (Курсы USD/EUR от НБРБ)"""
    logger.info("Загрузка списка автомобилей и обращение к внешним API Нацбанка")
    cars = Car.objects.all()

    usd_rate = 3.25
    eur_rate = 3.55
    
    try:
        usd_response = requests.get('https://developer.nbrb.by/exrates/rates/431', timeout=2)
        if usd_response.status_code == 200:
            usd_rate = usd_response.json().get('Cur_OfficialRate', usd_rate)
            logger.debug(f"Успешно получен курс USD: {usd_rate}")
            
        eur_response = requests.get('https://developer.nbrb.by/exrates/rates/451', timeout=2)
        if eur_response.status_code == 200:
            eur_rate = eur_response.json().get('Cur_OfficialRate', eur_rate)
            logger.debug(f"Успешно получен курс EUR: {eur_rate}")
            
    except requests.RequestException as e:
        logger.error(f"Ошибка при подключении к API Нацбанка РБ: {e}")

    for car in cars:
        price_in_byn = float(car.rental_price_per_day)
        car.price_usd = round(price_in_byn / usd_rate, 2)
        car.price_eur = round(price_in_byn / eur_rate, 2)

    search_query = request.GET.get('search', '')
    category_id = request.GET.get('category', '')
    sort_by = request.GET.get('sort', '')

    if search_query:
        logger.debug(f"Применен фильтр поиска по строке: '{search_query}'")
        cars = [c for c in cars if search_query.lower() in c.brand.lower() or search_query.lower() in c.model_name.lower()]

    if category_id:
        logger.debug(f"Применен фильтр по категории ID: {category_id}")
        cars = [c for c in cars if str(c.car_type_id) == str(category_id)]

    if sort_by == 'price_asc':
        cars = sorted(cars, key=lambda x: x.rental_price_per_day)
    elif sort_by == 'price_desc':
        cars = sorted(cars, key=lambda x: x.rental_price_per_day, reverse=True)
    elif sort_by == 'year_desc':
        cars = sorted(cars, key=lambda x: x.year, reverse=True)
    
    if sort_by:
        logger.debug(f"Применена сортировка: {sort_by}")

    categories = CarType.objects.all()
    
    context = {
        'cars': cars,
        'categories': categories,
        'search_query': search_query,
        'selected_category': category_id,
        'selected_sort': sort_by,
        'usd_rate': usd_rate,  
        'eur_rate': eur_rate,
    }
    return render(request, 'lab_5/cars_list.html', context)

def login_view(request):
    """Контроллер страницы входа"""
    if request.method == 'POST':
        form = AuthenticationForm(request, data=request.POST)
        if form.is_valid():
            user = form.get_user()
            login(request, user)
            logger.info(f"Пользователь '{user.username}' успешно авторизован в системе.")
            return redirect('lab_5:home') 
        else:
            logger.warning("Неудачная попытка входа: неверные учетные данные.")
    else:
        form = AuthenticationForm()
    
    return render(request, 'lab_5/login.html', {'form': form})


def logout_view(request):
    """Контроллер выхода из системы"""
    username = request.user.username if request.user.is_authenticated else "Аноним"
    logout(request)
    logger.info(f"Пользователь '{username}' вышел из системы.")
    return redirect('lab_5:home')


def register_view(request):
    """Контроллер страницы регистрации"""
    if request.method == 'POST':
        form = RegistrationForm(request.POST)
        if form.is_valid():
            user = form.save() 
            Client.objects.get_or_create(
                first_name=user.first_name or user.username,
                last_name=user.last_name or "Пользователь",
                defaults={
                    'age': form.calculated_age,
                    'address': 'Не указан',
                    'phone': '+375 (29) 000-00-00',
                }
            )
            login(request, user)
            logger.info(f"Зарегистрирован новый пользователь: '{user.username}'")
            return redirect('lab_5:home')
        else:
            logger.warning("Ошибка валидации формы при регистрации нового пользователя.")
    else:
        form = RegistrationForm()
    
    return render(request, 'lab_5/register.html', {'form': form})

from .forms import CarRentalForm, ReviewForm, ClientProfileForm  

@login_required(login_url='lab_5:login')
def profile_view(request):
    """Личный кабинет с разделением прав доступа (Сотрудник / Клиент) по ТЗ"""
    logger.info(f"Доступ к личному кабинету пользователя '{request.user.username}'")
    
    if request.user.groups.filter(name='Employees').exists() or request.user.is_superuser:
        logger.debug(f"Пользователь '{request.user.username}' распознан как персонал фирмы.")
        all_rentals = Rental.objects.all().order_by('-rent_date')
        all_clients = Client.objects.all()
        active_promos = PromoCode.objects.all()
        
        context = {
            'all_rentals': all_rentals,
            'all_clients': all_clients,
            'active_promos': active_promos,
            'is_employee': True
        }
        return render(request, 'lab_5/employee_profile.html', context)
        
    else:
        logger.debug(f"Пользователь '{request.user.username}' определен как клиент фирмы.")
        client, created = Client.objects.get_or_create(
            first_name=request.user.first_name or request.user.username,
            last_name=request.user.last_name or "Пользователь",
            defaults={'address': 'Не указан', 'phone': '+375 (29) 000-00-00', 'age': 20}
        )
        if created:
            logger.info(f"Создан новый связанный профиль Client для '{request.user.username}'")
        
        if request.method == 'POST':
            form = ClientProfileForm(request.POST, instance=client)
            if form.is_valid():
                form.save()
                logger.info(f"Профиль клиента '{request.user.username}' успешно обновлен.")
                return redirect('lab_5:profile')
            else:
                logger.warning(f"Ошибка изменения личных данных для '{request.user.username}': {form.errors}")
        else:
            form = ClientProfileForm(instance=client)
        
        my_rentals = Rental.objects.filter(client=client).order_by('-rent_date')
        active_promos = PromoCode.objects.filter(is_archived=False)
        
        context = {
            'client': client,
            'my_rentals': my_rentals,
            'active_promos': active_promos,
            'form': form,
            'is_employee': False
        }
        return render(request, 'lab_5/profile.html', context)

@login_required(login_url='lab_5:login')
def create_rental_view(request):
    """Оформление нового проката автомобиля"""
    client, _ = Client.objects.get_or_create(
        first_name=request.user.first_name or request.user.username,
        last_name=request.user.last_name or "Пользователь"
    )
    
    if request.method == 'POST':
        form = CarRentalForm(request.POST)
        if form.is_valid():
            rental = form.save(commit=False)
            rental.client = client
            rental.return_date = rental.rent_date + datetime.timedelta(days=rental.days_count)
            rental.total_price = rental.car.rental_price_per_day * rental.days_count
            rental.save()
            
            logger.info(f"Успешно добавлен новый заказ проката #{rental.id}. Машина: {rental.car}, дней: {rental.days_count}, сумма: {rental.total_price} BYN")
            return redirect('lab_5:profile')
        else:
            logger.warning(f"Ошибка создания проката пользователем '{request.user.username}': {form.errors}")
    else:
        form = CarRentalForm()
        
    return render(request, 'lab_5/create_rental.html', {'form': form})

@login_required(login_url='lab_5:login')
def edit_rental_view(request, rental_id):
    """Редактирование существующего заказа"""
    rental = get_object_or_404(Rental, id=rental_id)
    
    if rental.client.first_name != (request.user.first_name or request.user.username):
        logger.warning(f"Отказ в доступе: пользователь '{request.user.username}' пытался изменить чужой заказ #{rental_id}")
        return HttpResponse("Вы не можете редактировать чужие заказы!", status=403)

    if request.method == 'POST':
        form = CarRentalForm(request.POST, instance=rental)
        if form.is_valid():
            updated_rental = form.save(commit=False)
            updated_rental.return_date = updated_rental.rent_date + datetime.timedelta(days=updated_rental.days_count)
            updated_rental.total_price = updated_rental.car.rental_price_per_day * updated_rental.days_count
            updated_rental.save()
            
            logger.info(f"Заказ #{rental_id} изменен пользователем '{request.user.username}'. Новая сумма: {updated_rental.total_price} BYN")
            return redirect('lab_5:profile')
        else:
            logger.warning(f"Ошибка валидации при редактировании заказа #{rental_id}: {form.errors}")
    else:
        form = CarRentalForm(instance=rental)
        
    return render(request, 'lab_5/edit_rental.html', {'form': form, 'rental': rental})


@login_required(login_url='lab_5:login')
def delete_rental_view(request, rental_id):
    """Удаление/Отмена заказа (Операция Delete из CRUD)"""
    rental = get_object_or_404(Rental, id=rental_id)
    
    if rental.client.first_name != (request.user.first_name or request.user.username):
        logger.warning(f"Отказ в доступе: пользователь '{request.user.username}' пытался удалить чужой заказ #{rental_id}")
        return HttpResponse("Вы не можете отменять чужие заказы!", status=403)
        
    if request.method == 'POST':
        logger.info(f"Заказ #{rental_id} успешно аннулирован/удален пользователем '{request.user.username}'.")
        rental.delete()
        return redirect('lab_5:profile')
        
    return render(request, 'lab_5/delete_rental_confirm.html', {'rental': rental})

@login_required(login_url='lab_5:login')
@user_passes_test(lambda user: user.is_superuser, login_url='lab_5:login')
def statistics_view(request):
    """Контроллер для отображения статистики сайта, временных показателей и графиков Matplotlib"""
    logger.info(f"Формирование сводных финансовых показателей для '{request.user.username}'")
    
    rentals = Rental.objects.all()
    prices = [float(r.total_price) for r in rentals]
    
    mean_price = 0
    median_price = 0
    mode_price = 0
    
    if prices:
        mean_price = round(statistics.mean(prices), 2)
        median_price = round(statistics.median(prices), 2)
        try:
            mode_price = round(statistics.mode(prices), 2)
        except statistics.StatisticsError:
            mode_price = "Нет доминирующего значения"

    category_counts = {}
    for rental in rentals:
        if rental.car and rental.car.car_type:
            cat_name = rental.car.car_type.name
            category_counts[cat_name] = category_counts.get(cat_name, 0) + 1
    
    popular_category = "Данных пока нет"
    if category_counts:
        popular_category = max(category_counts, key=category_counts.get)

    clients = Client.objects.all()
    ages = [int(c.age) for c in clients if c.age]
    mean_age = 0
    median_age = 0
    if ages:
        mean_age = round(statistics.mean(ages), 1)
        median_age = round(statistics.median(ages), 1)

    chart_base64 = ""
    if category_counts:
        try:
            plt.figure(figsize=(6, 4))
            categories = list(category_counts.keys())
            counts = list(category_counts.values())
            
            plt.bar(categories, counts, color=['#4e73df', '#1cc88a', '#36b9cc', '#f6c23e', '#e74a3b'])
            plt.title('Распределение заказов по категориям автомобилей')
            plt.xlabel('Категории')
            plt.ylabel('Количество прокатов')
            plt.grid(axis='y', linestyle='--', alpha=0.7)
            plt.tight_layout()
            
            buffer = io.BytesIO()
            plt.savefig(buffer, format='png')
            buffer.seek(0)
            image_png = buffer.getvalue()
            buffer.close()
            plt.close() 
            
            chart_base64 = base64.b64encode(image_png).decode('utf-8')
            logger.debug("График распределения показателей успешно сгенерирован в Base64.")
        except Exception as ex:
            logger.error(f"Не удалось сгенерировать график распределения Matplotlib: {ex}")

    user_timezone_name = request.GET.get('tz') or timezone.get_current_timezone_name()
    try:
        user_timezone = ZoneInfo(user_timezone_name)
    except ZoneInfoNotFoundError:
        user_timezone_name = timezone.get_current_timezone_name()
        user_timezone = timezone.get_current_timezone()

    now_local = timezone.now()
    now_utc = datetime.datetime.now(datetime.timezone.utc)
    now_user_timezone = now_local.astimezone(user_timezone)
    
    current_year = now_user_timezone.year
    current_month = now_user_timezone.month
    
    text_cal = calendar.TextCalendar(firstweekday=0)
    html_calendar = text_cal.formatmonth(current_year, current_month)

    context = {
        'mean_price': mean_price,
        'median_price': median_price,
        'mode_price': mode_price,
        'popular_category': popular_category,
        'total_sales': sum(prices),
        
        'mean_age': mean_age,
        'median_age': median_age,
        
        'chart_image': chart_base64,
        
        'user_timezone_name': user_timezone_name,
        'now_local': now_user_timezone,
        'now_utc': now_utc,
        'html_calendar': html_calendar,
    }
    return render(request, 'lab_5/statistics.html', context)

def reviews_view(request):
    """Страница отзывов (вывод + обработка формы добавления)"""
    reviews = Review.objects.order_by('-created_at')
    
    if request.method == 'POST':
        if not request.user.is_authenticated:
            logger.warning("Анонимный пользователь пытался отправить отзыв в систему.")
            return redirect('lab_5:login') 
            
        form = ReviewForm(request.POST)
        if form.is_valid():
            review = form.save(commit=False)
            review.user_name = request.user.username  
            review.save()
            logger.info(f"Пользователь '{request.user.username}' оставил отзыв с оценкой {review.rating}.")
            return redirect('lab_5:reviews')
        else:
            logger.warning(f"Ошибка валидации текстовой формы отзыва: {form.errors}")
    else:
        form = ReviewForm()

    return render(request, 'lab_5/reviews.html', {'reviews': reviews, 'form': form})


def contacts_view(request):
    """Страница контактов (вывод сотрудников компании)"""
    logger.debug("Запрос страницы контактов")
    employees = Employee.objects.all()
    return render(request, 'lab_5/contacts.html', {'employees': employees})


def vacancies_view(request):
    """Страница вакансий"""
    logger.debug("Запрос страницы вакансий")
    vacancies = Vacancy.objects.all()
    return render(request, 'lab_5/vacancies.html', {'vacancies': vacancies})


def faq_view(request):
    """Страница словаря терминов / FAQ"""
    logger.debug("Запрос страницы FAQ")
    faqs = FAQ.objects.order_by('question')
    return render(request, 'lab_5/faq.html', {'faqs': faqs})


def privacy_view(request):
    """Страница-заглушка политики конфиденциальности по ТЗ"""
    logger.debug("Запрос страницы политики конфиденциальности")
    return render(request, 'lab_5/privacy.html')

def news_view(request):
    news_list = News.objects.order_by('-published_at')
    return render(request, 'lab_5/news.html', {'news_list': news_list})
