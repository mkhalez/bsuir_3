import datetime
from django.db import models
from django.core.validators import MinValueValidator, MaxValueValidator

class CarType(models.Model):
    """Тип кузова или категория автомобиля"""
    name = models.CharField(max_length=50, verbose_name="Название категории")

    class Meta:
        verbose_name = "Категория авто"
        verbose_name_plural = "Категории авто"

    def __str__(self):
        return self.name


class Car(models.Model):
    """Автомобиль из автопарка"""
    car_type = models.ForeignKey(CarType, on_delete=models.CASCADE, verbose_name="Тип кузова")
    license_plate = models.CharField(max_length=20, unique=True, verbose_name="Гос. номер")
    brand = models.CharField(max_length=50, verbose_name="Марка")
    model_name = models.CharField(max_length=50, verbose_name="Модель")
    year = models.PositiveIntegerField(verbose_name="Год выпуска")
    market_value = models.DecimalField(max_digits=10, decimal_places=2, verbose_name="Стоимость автомобиля")
    rental_price_per_day = models.DecimalField(max_digits=8, decimal_places=2, verbose_name="Суточная стоимость проката")

    class Meta:
        verbose_name = "Автомобиль"
        verbose_name_plural = "Автомобили"

    def __str__(self):
        return f"{self.brand} {self.model_name} ({self.license_plate})"


class CarTechnicalPassport(models.Model):
    """Технический паспорт автомобиля (Связь OneToOneField по ТЗ)"""
    car = models.OneToOneField(Car, on_delete=models.CASCADE, related_name='tech_passport', verbose_name="Автомобиль")
    vin_code = models.CharField(max_length=17, unique=True, verbose_name="VIN-код")
    engine_volume = models.DecimalField(max_digits=3, decimal_places=1, verbose_name="Объем двигателя (л)")
    color = models.CharField(max_length=30, verbose_name="Цвет")

    class Meta:
        verbose_name = "Техпаспорт"
        verbose_name_plural = "Техпаспорта"

    def __str__(self):
        return f"Техпаспорт для {self.car.brand} ({self.vin_code})"


class Discount(models.Model):
    """Система скидок"""
    name = models.CharField(max_length=100, verbose_name="Наименование скидки")
    percent = models.PositiveIntegerField(
        validators=[MinValueValidator(0), MaxValueValidator(100)],
        verbose_name="Процент скидки"
    )

    class Meta:
        verbose_name = "Скидка"
        verbose_name_plural = "Скидки"

    def __str__(self):
        return f"{self.name} ({self.percent}%)"


class Penalty(models.Model):
    """Система штрафов за возвращение автомобиля в ненадлежащем виде"""
    name = models.CharField(max_length=100, verbose_name="Наименование штрафа")
    amount = models.DecimalField(max_digits=10, decimal_places=2, verbose_name="Сумма штрафа")

    class Meta:
        verbose_name = "Штраф"
        verbose_name_plural = "Штрафы"

    def __str__(self):
        return f"{self.name} (+{self.amount} BYN)"


class Client(models.Model):
    """Клиент пункта проката"""
    first_name = models.CharField(max_length=50, verbose_name="Имя")
    last_name = models.CharField(max_length=50, verbose_name="Фамилия")
    middle_name = models.CharField(max_length=50, blank=True, null=True, verbose_name="Отчество")
    age = models.PositiveIntegerField(verbose_name="Возраст", default=18)
    address = models.CharField(max_length=255, verbose_name="Адрес")
    phone = models.CharField(max_length=20, verbose_name="Телефон (в формате +375...)")
    discounts = models.ManyToManyField(Discount, blank=True, verbose_name="Скидки")

    class Meta:
        verbose_name = "Клиент"
        verbose_name_plural = "Клиенты"

    def __str__(self):
        return f"{self.last_name} {self.first_name}"


class Rental(models.Model):
    """Фиксация факта проката (Заказ)"""
    car = models.ForeignKey(Car, on_delete=models.CASCADE, verbose_name="Автомобиль")
    client = models.ForeignKey(Client, on_delete=models.CASCADE, verbose_name="Клиент")
    rent_date = models.DateField(verbose_name="Дата выдачи")
    days_count = models.PositiveIntegerField(verbose_name="Количество дней проката")
    
    return_date = models.DateField(verbose_name="Ожидаемая дата возврата", null=True, blank=True)
    
    discount = models.ForeignKey(Discount, on_delete=models.SET_NULL, null=True, blank=True, verbose_name="Примененная скидка")
    penalty = models.ForeignKey(Penalty, on_delete=models.SET_NULL, null=True, blank=True, verbose_name="Примененный штраф")
    
    total_price = models.DecimalField(max_digits=10, decimal_places=2, default=0.00, verbose_name="Итоговая сумма проката")
    
    created_at = models.DateTimeField(auto_now_add=True, verbose_name="Дата создания записи")
    updated_at = models.DateTimeField(auto_now=True, verbose_name="Дата изменения записи")

    class Meta:
        verbose_name = "Прокат"
        verbose_name_plural = "Прокаты"

    def calculate_cost(self):
        """Бизнес-логика: Расчет стоимости с учетом года выпуска, скидок и штрафов"""
        base_rate = float(self.car.rental_price_per_day)
        
        if self.car.year >= 2025:
            base_rate *= 1.25 
        elif self.car.year < 2016:
            base_rate *= 0.85  
            
        raw_cost = base_rate * self.days_count

        discount_amount = 0
        if self.discount:
            discount_amount = raw_cost * (self.discount.percent / 100)
            
        penalty_amount = 0
        if self.penalty:
            penalty_amount = float(self.penalty.amount)
            
        return round(raw_cost - discount_amount + penalty_amount, 2)

    def save(self, *args, **kwargs):
        if not self.return_date:
            self.return_date = self.rent_date + datetime.timedelta(days=self.days_count)
        self.total_price = self.calculate_cost()
        super().save(*args, **kwargs)

    def __str__(self):
        return f"Прокат {self.car} для {self.client} от {self.rent_date}"


class CompanyInfo(models.Model):
    """О компании"""
    title = models.CharField(max_length=200, default="О нашей компании", verbose_name="Заголовок")
    text_content = models.TextField(verbose_name="Текст о компании")
    logo = models.ImageField(upload_to='company/', blank=True, null=True, verbose_name="Логотип (необязательно)")
    history_years = models.TextField(blank=True, verbose_name="История по годам")

    class Meta:
        verbose_name = "Информация о компании"
        verbose_name_plural = "Информация о компании"

    def __str__(self):
        return self.title


class News(models.Model):
    """Новости / Статьи"""
    title = models.CharField(max_length=200, verbose_name="Заголовок")
    short_description = models.CharField(max_length=255, verbose_name="Краткое содержание (одно предложение)")
    content = models.TextField(verbose_name="Полный текст статьи")
    image = models.ImageField(upload_to='news/', blank=True, null=True, verbose_name="Картинка")
    published_at = models.DateTimeField(auto_now_add=True, verbose_name="Дата публикации")

    class Meta:
        verbose_name = "Новость"
        verbose_name_plural = "Новости"

    def __str__(self):
        return self.title


class FAQ(models.Model):
    """Словарь терминов и понятий (Часто задаваемые вопросы)"""
    question = models.CharField(max_length=255, verbose_name="Вопрос / Термин")
    answer = models.TextField(verbose_name="Ответ / Определение")
    created_at = models.DateTimeField(auto_now_add=True, verbose_name="Дата добавления")

    class Meta:
        verbose_name = "Вопрос/Термин"
        verbose_name_plural = "Словарь терминов (FAQ)"

    def __str__(self):
        return self.question


class Employee(models.Model):
    """Контакты / Сотрудники"""
    first_name = models.CharField(max_length=50, verbose_name="Имя")
    last_name = models.CharField(max_length=50, verbose_name="Фамилия")
    position = models.CharField(max_length=100, verbose_name="Описание выполняемых работ / Должность")
    phone = models.CharField(max_length=20, verbose_name="Телефон")
    email = models.EmailField(verbose_name="Почта")
    age = models.PositiveIntegerField(verbose_name="Возраст", default=18)
    photo = models.ImageField(upload_to='employees/', blank=True, null=True, verbose_name="Фото сотрудника")

    class Meta:
        verbose_name = "Сотрудник"
        verbose_name_plural = "Сотрудники (Контакты)"

    def __str__(self):
        return f"{self.last_name} — {self.position}"


class Vacancy(models.Model):
    """Вакансии"""
    title = models.CharField(max_length=100, verbose_name="Название вакансии")
    description = models.TextField(verbose_name="Описание вакансии и требований")
    salary = models.CharField(max_length=50, blank=True, verbose_name="Заработная плата")

    class Meta:
        verbose_name = "Вакансия"
        verbose_name_plural = "Вакансии"

    def __str__(self):
        return self.title


class Review(models.Model):
    """Отзывы"""
    user_name = models.CharField(max_length=100, verbose_name="Имя автора")
    rating = models.PositiveIntegerField(
        validators=[MinValueValidator(1), MaxValueValidator(5)], 
        verbose_name="Оценка (1-5)"
    )
    text = models.TextField(verbose_name="Текст отзыва")
    created_at = models.DateTimeField(auto_now_add=True, verbose_name="Дата отзыва")

    class Meta:
        verbose_name = "Отзыв"
        verbose_name_plural = "Отзывы"

    def __str__(self):
        return f"Отзыв от {self.user_name} (Оценка: {self.rating})"


class PromoCode(models.Model):
    """Промокоды и купоны"""
    code = models.CharField(max_length=20, unique=True, verbose_name="Промокод")
    discount_amount = models.PositiveIntegerField(verbose_name="Размер скидки (%)")
    is_archived = models.BooleanField(default=False, verbose_name="В архиве?")

    class Meta:
        verbose_name = "Промокод/Купон"
        verbose_name_plural = "Промокоды и купоны"

    def __str__(self):
        status = "Архивный" if self.is_archived else "Активный"
        return f"{self.code} (-{self.discount_amount}%) [{status}]"