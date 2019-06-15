from django.urls import path

from . import views

urlpatterns = [
    path('', views.index, name='index'),
    path('zip/', views.zip, name='zip'),
]