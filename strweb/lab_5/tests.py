import datetime
from django.test import TestCase, Client
from django.urls import reverse
from django.contrib.auth.models import User, Group
from .models import Car, CarType, Client as RentalClient, Rental
from .forms import ClientProfileForm


class RentalModelsTestCase(TestCase):
    """Тестирование моделей и базовой логики подсчета стоимости"""

    def setUp(self):
        self.car_type = CarType.objects.create(name="Седан")
        
        self.car = Car.objects.create(
            car_type=self.car_type,
            license_plate="1111 AX-7",
            brand="Volkswagen",
            model_name="Polo",
            year=2020,
            market_value=15000,
            rental_price_per_day=50.00
        )
        
        self.client_user = RentalClient.objects.create(
            first_name="Иван",
            last_name="Иванов",
            middle_name="Иванович",
            age=25,
            address="Минск, ул. Гикало 9",
            phone="+375 (29) 123-45-67"
        )

    def test_rental_creation_and_fields(self):
        """Проверяем корректность создания проката и расчет дат/цен"""
        rent_date = datetime.date.today()
        days_count = 5
        
        rental = Rental.objects.create(
            car=self.car,
            client=self.client_user,
            rent_date=rent_date,
            days_count=days_count,
            total_price=self.car.rental_price_per_day * days_count,
            return_date=rent_date + datetime.timedelta(days=days_count)
        )

        self.assertEqual(rental.days_count, 5)
        self.assertEqual(rental.total_price, 250.00)
        self.assertEqual(rental.return_date, rent_date + datetime.timedelta(days=5))


class RentalFormsTestCase(TestCase):
    """Тестирование валидации форм (Бизнес-правила: возраст 18+, формат телефона)"""

    def test_client_profile_form_valid(self):
        """Форма должна быть валидной при корректных данных"""
        data = {
            'first_name': 'Петр',
            'last_name': 'Петров',
            'middle_name': 'Петрович',
            'age': 20,
            'address': 'Брест, ул. Советская 12',
            'phone': '+375 (29) 999-88-77'
        }
        form = ClientProfileForm(data=data)
        self.assertTrue(form.is_valid())

    def test_client_profile_form_underage(self):
        """Форма должна выдать ошибку, если возраст меньше 18 лет (Требование ТЗ)"""
        data = {
            'first_name': 'Мальчик',
            'last_name': 'Подросток',
            'age': 16,
            'address': 'Гомель',
            'phone': '+375 (29) 111-22-33'
        }
        form = ClientProfileForm(data=data)
        self.assertFalse(form.is_valid())
        self.assertIn('age', form.errors)
        self.assertEqual(form.errors['age'][0], "Вы должны быть старше 18 лет для регистрации в системе проката!")


class RentalViewsTestCase(TestCase):
    """Тестирование контроллеров (Views), авторизации и разграничения прав доступа"""

    def setUp(self):
        self.client = Client()
        self.user_client = User.objects.create_user(username='customer', password='password123')
        self.user_employee = User.objects.create_user(username='manager', password='password123')
        self.employee_group = Group.objects.create(name='Employees')
        self.user_employee.groups.add(self.employee_group)
        
        self.car_type = CarType.objects.create(name="Электромобиль")
        self.car = Car.objects.create(
            car_type=self.car_type, license_plate="7777 MI-7", brand="Tesla", 
            model_name="Model S", year=2023, market_value=50000, rental_price_per_day=120
        )

    def test_cars_list_view_anonymous(self):
        """Неавторизованный пользователь должен иметь доступ к списку авто"""
        response = self.client.get(reverse('lab_5:cars_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Tesla")

    def test_create_rental_view_redirects_anonymous(self):
        """Попытка зайти на создание проката без авторизации должна редиректить на login"""
        response = self.client.get(reverse('lab_5:create_rental'))
        self.assertEqual(response.status_code, 302)

    def test_profile_view_for_client(self):
        """Обычный пользователь видит профиль клиента"""
        self.client.login(username='customer', password='password123')
        response = self.client.get(reverse('lab_5:profile'))
        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, 'lab_5/profile.html')


class StatisticsTestCase(TestCase):
    """Тестирование работы математической статистики и генерации Matplotlib"""

    def setUp(self):
        self.user_employee = User.objects.create_user(username='stat_manager', password='password123', is_staff=True)
        
        self.car_type = CarType.objects.create(name="Седан")
        self.car = Car.objects.create(
            car_type=self.car_type, license_plate="5555 BT-7", brand="Geely", 
            model_name="Emgrand", year=2024, market_value=20000, rental_price_per_day=60
        )
        self.customer = RentalClient.objects.create(
            first_name="Василий", last_name="Пупкин", age=30, phone="+375 (29) 111-11-11"
        )
        
        Rental.objects.create(car=self.car, client=self.customer, rent_date=datetime.date.today(), days_count=2, total_price=120.00)
        Rental.objects.create(car=self.car, client=self.customer, rent_date=datetime.date.today(), days_count=2, total_price=120.00)
        Rental.objects.create(car=self.car, client=self.customer, rent_date=datetime.date.today(), days_count=5, total_price=300.00)

    def test_statistics_calculations(self):
        """Проверяем правильность расчета средних значений коммерческой службы"""
        self.client.login(username='stat_manager', password='password123')
        
        response = self.client.get(reverse('lab_5:statistics'))
        self.assertEqual(response.status_code, 200)
        
        self.assertEqual(response.context['total_sales'], 540.00)
        self.assertEqual(response.context['mean_price'], 180.00)
        self.assertEqual(response.context['mode_price'], 120.00)
        self.assertEqual(response.context['popular_category'], "Седан")
        self.assertNotEqual(response.context['chart_image'], "")