from threading import Thread

import cv2
from Crypto.Cipher import AES
import os
import time
import zipfile

ROLLING_IMAGE_CARDINALITY = 20
FRAMES_PER_SECOND = 1
SECONDS_PER_FRAME = 1. / float(FRAMES_PER_SECOND)
RESOURCES_DIR = 'res/'
IMAGE_FILE_TYPE = '.jpg'


class WebcamManager(Thread):

    def __init__(self):
        super().__init__()
        self.captures = [None] * ROLLING_IMAGE_CARDINALITY
        self.capture_ind = 0
        self.paused = False
        self.cam = cv2.VideoCapture(-1)

    def run(self):
        print('Starting webcam service')

        files_in_resources = [RESOURCES_DIR + x for x in os.listdir(RESOURCES_DIR) if IMAGE_FILE_TYPE in x]
        for x in files_in_resources:
            os.remove(x)

        while(True):
            #print('start')
            if self.paused:
                continue
            start_time = time.time()
            try:
                frame = self.capture()
                frame = self.rotate180(frame)

                img_path = RESOURCES_DIR + str(start_time)[:14] + IMAGE_FILE_TYPE
                cv2.imwrite(img_path,frame)

                to_overwrite = self.captures[self.capture_ind]

                self.captures[self.capture_ind] = img_path
                self.capture_ind += 1
                self.capture_ind %= len(self.captures)
                if (to_overwrite != None):
                    os.remove(to_overwrite)
                    #print('removing ' + to_overwrite)
            except Exception as e:
                print(str(e))
            duration = time.time() - start_time
            to_sleep = max(0.,SECONDS_PER_FRAME - duration)
            time.sleep(to_sleep)
            #print('sleeping ' + str(to_sleep))
        self.cam.release()

    def capture(self):
        return_code, frame = self.cam.read()
        return frame

    def rotate180(self,img):
        (height, width) = img.shape[:2]
        M = cv2.getRotationMatrix2D((width/2,height/2),180,1)
        return cv2.warpAffine(img,M,(width,height))

    def get(self,ind): # returns binary data of requested image file
        actual_ind = (self.capture_ind - ind - 1) % len(self.captures) #-1 since the next position to place a frame is pointed to by capture_ind, not the last index WITH a frame.
        #print('getting ' + str(actual_ind))
        if self.captures[actual_ind] == None:
            return None
        else:
            try:
                with open(self.captures[actual_ind],'rb') as img_file:
                    return img_file.read()
            except:
                return None

    def get_zip_of_all_files(self): # returns binary data of zip file
        print('Zipping files')
        files_to_zip = [x for x in self.captures if x != None]
        zip_file_name = RESOURCES_DIR + 'caps.zip'
        if os.path.exists(zip_file_name):
            os.remove(zip_file_name)
        with zipfile.ZipFile(zip_file_name,'w') as zip:
            for x in files_to_zip:
                zip.write(x)
        with open(zip_file_name, 'rb') as file:
            return file.read(),zip_file_name


class EncryptionManager:
    def __init__(self):
        with open('res/password.txt', 'r') as pwd_file:
            password = pwd_file.readline()
        self.key = self.password_to_key(password)

    def encrypt(self,maybe_bytes):
        data = maybe_bytes
        if not hasattr(maybe_bytes,'decode'):
            data = bytes(maybe_bytes,'utf-8')
        iv = os.urandom(16)
        cipher = AES.new(self.key,AES.MODE_CBC,iv)
        ciphertext = cipher.encrypt(data)#cipher.encrypt_and_digest(data)
        return  ciphertext, iv

    def decrypt(self, bytes):
        iv = bytes[:16]
        unpadded_bytes = int.from_bytes(bytes[16:48], byteorder='big')
        ciphertext = bytes[48:]
        cipher = AES.new(self.key,AES.MODE_CBC,iv)#, nonce = nonce)
        data = cipher.decrypt(ciphertext)
        return data[:unpadded_bytes]

    def fill_bytes(self,bts,size):
        fill = bytearray(size)
        for x in range(min(len(bts), size)):
            fill[x] = bts[x]
        return bytes(fill)

    def fill_bytes_to_dword(self,bytes):
        return self.fill_bytes(bytes,32)

    def password_to_key(self,password):
        b = bytes(password,'utf-8')
        return self.fill_bytes(b,16)

    def pad_bytes_to_multiple_of_16(self,bts):
        pad_up_to = (int(len(bts) / 16) + 1) * 16
        diff = pad_up_to - len(bts)
        padded = self.fill_bytes(bts,pad_up_to)
        return padded, diff

    def get_aes_packet(self,data):
        if data == None:
            return None
        print('unpadded: ' + str(len(data)))
        padded_data, amount_padding = self.pad_bytes_to_multiple_of_16(data)
        print('padded: ' + str(len(padded_data)))
        ciphertext, iv = self.encrypt(padded_data)  # iv is 16 bytes
        len_bytes = len(data).to_bytes(32, byteorder='big')
        return iv + len_bytes + ciphertext