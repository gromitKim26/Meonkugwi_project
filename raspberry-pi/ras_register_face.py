# -*- coding: utf-8 -*-
import cv2
import os
import numpy as np
import subprocess
from ultralytics import YOLO

# 모델 경로 설정
MODEL_PATH = "yolov8n-face.pt"
yolo_model = YOLO(MODEL_PATH)

# 얼굴 저장 폴더 생성
FACE_DIR = "face_data"
os.makedirs(FACE_DIR, exist_ok=True)

# libcamera-vid 명령어로 카메라 스트리밍
cmd = [
    "libcamera-vid",
    "--width", "1280",
    "--height", "720",
    "--framerate", "15",
    "--codec", "yuv420",
    "--nopreview",
    "--inline",
    "--timeout", "0",
    "-o", "-"
]

proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)

width, height = 1280, 720
frame_size = width * height * 3 // 2
buffer = b""
face_count = 0
MAX_FACES = 3

print("자전거 주인 얼굴 등록을 시작합니다.")
print("정면 → 왼쪽 측면 → 오른쪽 측면 순서로 카메라를 봐주세요.")
print("각 방향에서 's' 키를 눌러 저장, 'q' 누르면 취소.")

cv2.namedWindow("Camera Feed", cv2.WINDOW_NORMAL)
cv2.resizeWindow("Camera Feed", 960, 720)

try:
    while face_count < MAX_FACES:
        # 버퍼에서 프레임 수신
        while len(buffer) < frame_size:
            buffer += proc.stdout.read(frame_size - len(buffer))

        yuv = np.frombuffer(buffer[:frame_size], dtype=np.uint8).reshape((height * 3 // 2, width))
        buffer = buffer[frame_size:]

        # 색상 복원 (YUV → BGR)
        frame = cv2.cvtColor(yuv, cv2.COLOR_YUV2BGR_I420)

        # YOLO로 얼굴 감지
        results = yolo_model(frame, imgsz=640)

        # 감지된 결과 그리기
        annotated = results[0].plot()
        cv2.imshow("Camera Feed", annotated)

        # 저장은 원본 frame 전체로
        key = cv2.waitKey(1)
        if key == ord('s'):
            face_count += 1
            face_path = os.path.join(FACE_DIR, f"owner{face_count}.jpg")
            cv2.imwrite(face_path, frame)  # 전체 프레임 저장
            print(f"{face_count}번째 전체 화면이 저장되었습니다: {face_path}")

            if face_count == MAX_FACES:
                print("모든 얼굴이 등록되었습니다!")
                break

        if key == ord('q'):
            print("얼굴 등록이 취소되었습니다.")
            break

except KeyboardInterrupt:
    print("중단됨")

finally:
    proc.terminate()
    cv2.destroyAllWindows()
