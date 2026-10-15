import os
import sys
import socket

# Set generous socket timeout for large file uploads
socket.setdefaulttimeout(1200)

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

_orig_print = print
def print(*args, **kwargs):
    kwargs.setdefault('flush', True)
    _orig_print(*args, **kwargs)

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PACKAGE_NAME = 'com.auracast.weather'
KEY_FILE = os.path.join('fastlane', 'play-service-account.json')
AAB_PATH = os.path.join('app', 'build', 'outputs', 'bundle', 'release', 'app-release.aab')

def main():
    print(f"Loading service account key from {KEY_FILE}...")
    if not os.path.exists(KEY_FILE):
        print(f"Error: Key file {KEY_FILE} not found!")
        sys.exit(1)

    credentials = service_account.Credentials.from_service_account_file(
        KEY_FILE,
        scopes=['https://www.googleapis.com/auth/androidpublisher']
    )
    service = build('androidpublisher', 'v3', credentials=credentials)

    print(f"Creating new edit for package {PACKAGE_NAME}...")
    edit_request = service.edits().insert(packageName=PACKAGE_NAME, body={})
    edit = edit_request.execute()
    edit_id = edit['id']
    print(f"Created edit with ID: {edit_id}")

    if not os.path.exists(AAB_PATH):
        print(f"Error: AAB file not found at {AAB_PATH}")
        sys.exit(1)

    file_size_mb = os.path.getsize(AAB_PATH) / (1024 * 1024)
    print(f"Uploading AAB ({file_size_mb:.1f} MB) in 5MB chunks...")

    # Use 5MB chunk size for reliable chunked progress
    media = MediaFileUpload(
        AAB_PATH,
        mimetype='application/octet-stream',
        chunksize=5 * 1024 * 1024,
        resumable=True
    )

    request = service.edits().bundles().upload(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        media_body=media,
        ackBundleInstallationWarning=True
    )

    response = None
    while response is None:
        status, response = request.next_chunk()
        if status:
            print(f"  Upload progress: {int(status.progress() * 100)}%")

    version_code = response['versionCode']
    print(f"Successfully uploaded AAB bundle with versionCode: {version_code}!")

    print("\nAssigning bundle to 'production' track as DRAFT...")
    track_body = {
        'track': 'production',
        'releases': [{
            'name': f'v0.1.0 ({version_code})',
            'versionCodes': [version_code],
            'status': 'draft',
            'releaseNotes': [{
                'language': 'en-US',
                'text': 'AuraCast Weather launch: On-device AI weather podcast briefing, live animated radar, and instant 59-language offline translation.'
            }]
        }]
    }

    service.edits().tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track='production',
        body=track_body
    ).execute()
    print("Release assigned to Production track as DRAFT.")

    print("\nCommitting edit to Google Play Console...")
    commit_res = service.edits().commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(f"SUCCESS! Edit {edit_id} committed. Release v0.1.0 ({version_code}) is now in Google Play Production as a DRAFT!")

if __name__ == '__main__':
    main()
