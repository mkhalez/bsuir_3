import datetime
import re
from django import forms
from django.contrib.auth.forms import UserCreationForm
from django.core.exceptions import ValidationError
from django.core.validators import RegexValidator
from .models import Rental, Car, Review, Client


class RegistrationForm(UserCreationForm):
    """Форма регистрации с обязательным ограничением возраста 18+"""
    birth_date = forms.DateField(
        label="Дата рождения",
        widget=forms.DateInput(attrs={'type': 'date'}),
        error_messages={
            'required': "Укажите вашу дату рождения.",
        },
    )

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        today = datetime.date.today()
        max_birth_date = today.replace(year=today.year - 18)
        self.fields['birth_date'].widget.attrs['max'] = max_birth_date.strftime('%Y-%m-%d')

    def clean_birth_date(self):
        birth_date = self.cleaned_data.get('birth_date')
        today = datetime.date.today()
        max_birth_date = today.replace(year=today.year - 18)

        if birth_date > today:
            raise ValidationError("Дата рождения не может быть в будущем.")

        age = today.year - birth_date.year - (
            (today.month, today.day) < (birth_date.month, birth_date.day)
        )
        if birth_date > max_birth_date or age < 18:
            raise ValidationError("Регистрация доступна только пользователям от 18 лет.")

        self.calculated_age = age
        return birth_date

class CarRentalForm(forms.ModelForm):
    """Форма для оформления проката автомобиля зарегистрированным клиентом"""
    class Meta:
        model = Rental
        fields = ['car', 'rent_date', 'days_count']
        widgets = {
            'rent_date': forms.DateInput(attrs={'type': 'date', 'min': datetime.date.today().strftime('%Y-%m-%d')}),
            'days_count': forms.NumberInput(attrs={'min': 1, 'max': 30}),
        }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields['car'].queryset = Car.objects.all()


class ReviewForm(forms.ModelForm):
    """Форма добавления отзыва на сайте"""
    class Meta:
        model = Review
        fields = ['text', 'rating']
        widgets = {
            'text': forms.Textarea(attrs={'rows': 4, 'placeholder': 'Напишите ваш отзыв здесь...'}),
            'rating': forms.Select(choices=[(i, str(i)) for i in range(1, 6)]),
        }


class ClientProfileForm(forms.ModelForm):
    """Форма редактирования профиля клиента с валидацией по ТЗ"""
    
    phone = forms.CharField(
        max_length=20,
        label="Телефон",  
        validators=[
            RegexValidator(
                regex=r'^\+375 \((29|33|44|25)\) \d{3}-\d{2}-\d{2}$',
                message="Номер телефона должен быть в формате +375 (29) XXX-XX-XX"
            )
        ],
        widget=forms.TextInput(attrs={'placeholder': '+375 (29) 123-45-67'})
    )

    class Meta:
        model = Client
        fields = ['first_name', 'last_name', 'middle_name', 'age', 'address', 'phone']

    def clean_age(self):
        """Проверка возрастного ограничения 18+"""
        age = self.cleaned_data.get('age')
        if age is not None and age < 18:
            raise ValidationError("Вы должны быть старше 18 лет для регистрации в системе проката!")
        return age
