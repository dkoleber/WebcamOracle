from django.http import HttpResponse
from django.shortcuts import render
from webcam_manager import *
import time


webcam_manager = WebcamManager()
encryption_manager = EncryptionManager()

webcam_manager.start()

def make_aes_response(response_data):
    response = encryption_manager.get_aes_packet(response_data)

    if response == None:
        return HttpResponse(status=500)
    else:
        return HttpResponse(response, content_type='application/octet-stream')



def index(request):
    try:
        image_data = webcam_manager.get(0) # get most recent image
        return make_aes_response(image_data)
    except:
        return HttpResponse(status=500)

def get_zip(request):
    try:
        zip_data, zip_name  = webcam_manager.get_zip_of_all_files()
        return make_aes_response(zip_data)
    except:
        return HttpResponse(status=500)
