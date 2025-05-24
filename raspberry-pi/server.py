# -*- coding: utf-8 -*-
from flask import Flask, request, jsonify, send_from_directory
from werkzeug.utils import secure_filename
from PIL import Image, ExifTags
import os, uuid
import requests
from google.oauth2 import service_account
import google.auth.transport.requests

app = Flask(__name__)

UPLOAD_FOLDER = 'registered_faces'
UPLOAD_VIDEO_FOLDER = 'videos'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(UPLOAD_VIDEO_FOLDER, exist_ok=True)

SERVICE_ACCOUNT_FILE = '/home/pi/fcm_service_account.json'  # ← 여기에 서비스 키 파일 경로

PROJECT_ID = 'lovebikealert'  # ← Firebase 콘솔의 프로젝트 ID

def correct_image_orientation(image):
    try:
        for orientation in ExifTags.TAGS.keys():
            if ExifTags.TAGS[orientation] == 'Orientation':
                break
        exif = image._getexif()
        if exif is not None:
            orientation_value = exif.get(orientation, None)
            if orientation_value == 3:
                image = image.rotate(180, expand=True)
            elif orientation_value == 6:
                image = image.rotate(270, expand=True)
            elif orientation_value == 8:
                image = image.rotate(90, expand=True)
    except Exception as e:
        print(f"EXIF 회전 보정 오류: {e}")
    return image

@app.route('/register-face', methods=['POST'])
def register_face():
    if 'image' not in request.files:
        return jsonify({'error': '이미지 파일이 포함되지 않았습니다'}), 400

    image_file = request.files['image']
    if image_file.filename == '':
        return jsonify({'error': '파일 이름이 비어 있습니다'}), 400

    filename = f"{uuid.uuid4().hex}.jpg"
    save_path = os.path.join(UPLOAD_FOLDER, filename)

    try:
        image = Image.open(image_file)
        image = correct_image_orientation(image)
        image.save(save_path)
        print(f"얼굴 이미지 저장 완료: {save_path}")
        return jsonify({'message': '얼굴 등록 성공', 'filename': filename}), 200
    except Exception as e:
        return jsonify({'error': f'이미지 저장 실패: {e}'}), 500

@app.route('/upload-video', methods=['POST'])
def upload_video():
    if 'video' not in request.files:
        return jsonify({'error': 'video 파일이 포함되지 않았습니다'}), 400

    video_file = request.files['video']
    filename = secure_filename(video_file.filename)
    save_path = os.path.join(UPLOAD_VIDEO_FOLDER, filename)
    video_file.save(save_path)

    print(f"🎥 영상 업로드 완료: {save_path}")
    return jsonify({
        'message': '업로드 성공',
        'url': f"http://{request.host}/videos/{filename}"
    }), 200

@app.route('/videos/list', methods=['GET'])
def list_videos():
    video_files = [
        f"http://{request.host}/videos/{f}"
        for f in os.listdir(UPLOAD_VIDEO_FOLDER)
        if f.endswith('.avi')
    ]
    return jsonify(video_files)

@app.route('/videos/<filename>')
def get_video(filename):
    return send_from_directory(UPLOAD_VIDEO_FOLDER, filename)

def get_access_token():
    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE,
        scopes=['https://www.googleapis.com/auth/firebase.messaging']
    )
    auth_req = google.auth.transport.requests.Request()
    credentials.refresh(auth_req)
    return credentials.token

def send_push_notification(title, body, video_url):
    access_token = get_access_token()
    url = f"https://fcm.googleapis.com/v1/projects/{PROJECT_ID}/messages:send"
    headers = {
        'Authorization': f'Bearer {access_token}',
        'Content-Type': 'application/json; UTF-8',
    }

    message = {
        "message": {
            "topic": "alert",
            "notification": {
                "title": title,
                "body": body
            },
            "data": {
                "title": title,
                "body": body,
                "video_url": video_url  # ✅ 영상 URL 포함
            }
        }
    }

    response = requests.post(url, headers=headers, json=message)
    print("✅ FCM 응답:", response.status_code, response.text)

@app.route('/alert', methods=['POST'])
def alert():
    # 가장 최근 영상 파일 찾기
    files = sorted(
        [f for f in os.listdir(UPLOAD_VIDEO_FOLDER) if f.endswith('.avi')],
        key=lambda x: os.path.getmtime(os.path.join(UPLOAD_VIDEO_FOLDER, x)),
        reverse=True
    )
    if not files:
        return jsonify({'error': '영상 없음'}), 400

    latest_video = files[0]
    video_url = f"http://{request.host}/videos/{latest_video}"

    send_push_notification(
        title="🚨 도난 감지",
        body="미등록자가 자전거에 접근했습니다!",
        video_url=video_url
    )
    return jsonify({'message': '알림 전송됨', 'video_url': video_url}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
