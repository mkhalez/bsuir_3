from django.contrib import admin
from .models import (
    CarType, Car, Discount, Client, Review, Employee, 
    Vacancy, FAQ, Rental, News, CompanyInfo, CarTechnicalPassport, PromoCode, Penalty
)

admin.site.register(CarType)
admin.site.register(CarTechnicalPassport)
admin.site.register(Discount)
admin.site.register(CompanyInfo)
admin.site.register(News) 
admin.site.register(FAQ)
admin.site.register(Employee)
admin.site.register(Vacancy)
admin.site.register(Review)
admin.site.register(PromoCode)
admin.site.register(Penalty)

@admin.register(Car)
class CarAdmin(admin.ModelAdmin):
    list_display = ('brand', 'model_name', 'license_plate', 'year', 'rental_price_per_day')
    list_filter = ('year', 'car_type')
    search_fields = ('brand', 'model_name', 'license_plate')


@admin.register(Client)
class ClientAdmin(admin.ModelAdmin):
    list_display = ('last_name', 'first_name', 'phone', 'address')
    search_fields = ('last_name', 'phone')
    filter_horizontal = ('discounts',)


@admin.register(Rental)
class RentalAdmin(admin.ModelAdmin):
    list_display = ('car', 'client', 'rent_date', 'days_count', 'total_price')
    list_filter = ('rent_date',)