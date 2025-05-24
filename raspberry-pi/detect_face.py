from picamera2 import Picamera2
import cv2
import face_recognition
import time
import os
from datetime import datetime
import requests

# 설정
REGISTERED_DIR = 'registered_faces'
RECORD_SECONDS = 5
VIDEO_SIZE = (640, 480)
SERVER_UPLOAD_URL = 'http://localhost:5000/upload-video'
SERVER_ALERT_URL = 'http://localhost:5000/alert'

# 등록된 얼굴 불러오기
registered_encodings = []
for filename in os.listdir(REGISTERED_DIR):
    path = os.path.join(REGISTERED_DIR, filename)
    image = face_recognition.load_image_file(path)
    encodings = face_recognition.face_encodings(image)
    if encodings:
        registered_encodings.append(encodings[0])

# 카메라 초기화
picam2 = Picamera2()
picam2.preview_configuration.main.size = VIDEO_SIZE
picam2.preview_configuration.main.format = "RGB888"
picam2.configure("preview")
picam2.start()

unknown_start_time = None

print("🚨 실시간 감시 시작...")

while True:
    frame = picam2.capture_array()
    rgb_frame = frame  # 이미 RGB 형식

    face_locations = face_recognition.face_locations(rgb_frame)
    face_encodings = face_recognition.face_encodings(rgb_frame, face_locations)

    is_known = False
    for encoding in face_encodings:
        results = face_recognition.compare_faces(registered_encodings, encoding, tolerance=0.5)
        if True in results:
            is_known = True
            break

    if is_known:
        print("✅ 주인 인식됨")
        unknown_start_time = None
    elif face_encodings:
        print("❌ 미등록자 감지 중...")
        if unknown_start_time is None:
            unknown_start_time = time.time()
        elif time.time() - unknown_start_time >= 5:
            print("🚨 미등록자 지속 감지 → 녹화 시작")

            # 영상 저장 준비
            fourcc = cv2.VideoWriter_fourcc(*'XVID')
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"suspect_{timestamp}.avi"
            out = cv2.VideoWriter(filename, fourcc, 20.0, VIDEO_SIZE)

            record_end = time.time() + RECORD_SECONDS
            while time.time() < record_end:
                frame = picam2.capture_array()
                out.write(frame)
                print("📹 녹화 중...")

            out.release()
            print(f"✅ 녹화 완료: {filename}")

            # 업로드
            with open(filename, 'rb') as f:
                files = {'video': (filename, f, 'video/avi')}
                response = requests.post(SERVER_UPLOAD_URL, files=files)
                print("⬆️ 영상 업로드 응답:", response.status_code)

            # 알림 전송
            try:
                response = requests.post(SERVER_ALERT_URL)
                print("📲 알림 전송 응답:", response.status_code)
            except Exception as e:
                print("❌ 알림 전송 실패:", e)

            # 다시 감시 상태로 리셋
            unknown_start_time = None

    time.sleep(0.2)
