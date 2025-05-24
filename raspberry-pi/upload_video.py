import requests

video_path = 'suspect_20250524_105111.avi'  # 실제 파일 이름으로 바꿔줘
url = 'http://192.168.8.234:5000/upload-video'  # 서버 주소에 맞게 수정

with open(video_path, 'rb') as f:
    files = {'video': (video_path, f, 'video/avi')}
    response = requests.post(url, files=files)

# 상태 코드와 응답 본문 먼저 확인
print("응답 상태 코드:", response.status_code)
print("응답 본문:", response.text)

# 그 다음에 JSON 파싱 시도 (성공할 경우만)
try:
    print("JSON 응답:", response.json())
except Exception as e:
    print("⚠️ JSON 파싱 실패:", e)