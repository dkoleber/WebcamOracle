from django.urls import path

from . import views

urlpatterns = [
    path('', views.index, name='index'),
    path('zip/', views.get_zip, name='zip')
]