import os
import cv2
import time
import numpy as np
import subprocess
import face_recognition
from ultralytics import YOLO

# YOLO 얼굴 모델 로드
MODEL_PATH = "yolov8n-face.pt"
yolo_model = YOLO(MODEL_PATH)

# 등록된 얼굴 인코딩 로드
FACE_DIR = "face_data"
owner_encodings = []
for i in range(1, 4):
    img_path = os.path.join(FACE_DIR, f"owner{i}.jpg")
    if os.path.exists(img_path):
        img = face_recognition.load_image_file(img_path)
        enc = face_recognition.face_encodings(img)
        if enc:
            owner_encodings.append(enc[0])

# libcamera-vid로 YUV420 스트림 받기
cmd = [
    "libcamera-vid",
    "--width", "640",
    "--height", "480",
    "--framerate", "15",
    "--codec", "yuv420",
    "--nopreview",
    "--inline",
    "--timeout", "0",
    "-o", "-"
]

proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)
frame_size = 640 * 480 * 3 // 2
buffer = b""
start_time = None
ALERT_THRESHOLD = 5

cv2.namedWindow("Theft Detection", cv2.WINDOW_NORMAL)

print("🚲 도난 감지 시스템 시작!")

try:
    while True:
        while len(buffer) < frame_size:
            chunk = proc.stdout.read(frame_size - len(buffer))
            if not chunk:
                raise RuntimeError("카메라 데이터 수신 실패")
            buffer += chunk

        yuv = np.frombuffer(buffer[:frame_size], dtype=np.uint8).reshape((480 * 3 // 2, 640))
        buffer = buffer[frame_size:]
        frame = cv2.cvtColor(yuv, cv2.COLOR_YUV2BGR_I420)
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

        results = yolo_model(frame, imgsz=416)
        boxes = results[0].boxes

        face_locations = []
        for box in boxes:
            x, y, w, h = map(int, box.xywh[0])
            top = max(y - h // 2, 0)
            bottom = min(y + h // 2, frame.shape[0])
            left = max(x - w // 2, 0)
            right = min(x + w // 2, frame.shape[1])
            if (right - left) > 50 and (bottom - top) > 50:
                face_locations.append((top, right, bottom, left))

        face_encodings = face_recognition.face_encodings(rgb, face_locations)
        unauthorized = True
        for encoding in face_encodings:
            matches = face_recognition.compare_faces(owner_encodings, encoding, tolerance=0.4)
            if any(matches):
                unauthorized = False
                break

        if unauthorized and face_encodings:
            cv2.putText(frame, "Unauthorized!", (30, 40), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
            if start_time is None:
                start_time = time.time()
            elif time.time() - start_time >= ALERT_THRESHOLD:
                print("도난 경고! 앱 알림 전송")
                cv2.putText(frame, "THEFT ALERT", (120, 80), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
        else:
            cv2.putText(frame, "Owner Verified", (30, 40), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
            start_time = None

        for (top, right, bottom, left) in face_locations:
            cv2.rectangle(frame, (left, top), (right, bottom), (255, 0, 0), 2)

        cv2.imshow("Theft Detection", frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

except KeyboardInterrupt:
    print("감지 중단됨")

finally:
    proc.terminate()
    cv2.destroyAllWindows()
