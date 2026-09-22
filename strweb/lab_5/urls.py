from django.urls import path
from . import views

app_name = 'lab_5'

urlpatterns = [
    path('', views.home_view, name='home'),
    path('about/', views.about_view, name='about'),
    path('cars/', views.cars_list_view, name='cars_list'),
    path('login/', views.login_view, name='login'),
    path('logout/', views.logout_view, name='logout'),
    path('register/', views.register_view, name='register'),
    path('profile/', views.profile_view, name='profile'),
    path('rental/new/', views.create_rental_view, name='create_rental'),
    path('rental/edit/<int:rental_id>/', views.edit_rental_view, name='edit_rental'),
    path('rental/delete/<int:rental_id>/', views.delete_rental_view, name='delete_rental'),
    path('stats/', views.statistics_view, name='statistics'),
    path('reviews/', views.reviews_view, name='reviews'),
    path('contacts/', views.contacts_view, name='contacts'),
    path('vacancies/', views.vacancies_view, name='vacancies'),
    path('faq/', views.faq_view, name='faq'),
    path('privacy/', views.privacy_view, name='privacy'),
    path('news/', views.news_view, name='news'),
]