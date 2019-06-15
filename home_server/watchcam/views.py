from django.http import HttpResponse
from django.shortcuts import render
import http.client
from webcam_manager import *
import time

encryption_manager = EncryptionManager()

def unencrypt_request(request, content_type, content_disposition=None):
    try:
        url = '192.168.1.13:8000'
        connection = http.client.HTTPConnection(url)
        path = str.replace(request.path,'/w/','/cam/')
        connection.request('GET',path)

        response = connection.getresponse()

        if response.status == 200:
            data = response.read()
            decrypted = encryption_manager.decrypt(data)
            forwarded_response = HttpResponse(decrypted, content_type=content_type)
            if content_disposition != None:
                forwarded_response['Content-Disposition'] = content_disposition
            return forwarded_response
        else:
            return HttpResponse('server returned ' + str(response.status))
    except Exception as e:
        print(e)
        return HttpResponse('server forward failed',status=500)


def index(request):
    return unencrypt_request(request,'image/jpeg')

def zip(request):
    content_disposition = 'attachment; filename=captures_' + str(time.time())[:14] + '.zip'
    return unencrypt_request(request, 'application/force-download', content_disposition)


