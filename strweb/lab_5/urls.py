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
    path('news/<int:news_id>/', views.news_detail_view, name='news_detail'),
    path('cars/<int:car_id>/', views.car_detail_view, name='car_detail'),
    path('cart/', views.cart_view, name='cart'),
    path('cart/add/<int:car_id>/', views.cart_add_view, name='cart_add'),
    path('cart/update/<int:car_id>/', views.cart_update_view, name='cart_update'),
    path('cart/remove/<int:car_id>/', views.cart_remove_view, name='cart_remove'),
    path('checkout/', views.checkout_view, name='checkout'),
    path('promocodes/', views.promocodes_view, name='promocodes'),
    path('lr1/', views.lr1_demo_view, name='lr1_demo'),
]