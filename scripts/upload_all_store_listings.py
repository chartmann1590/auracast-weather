import os
import sys
import glob
import time
import socket

socket.setdefaulttimeout(300)

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

_orig_print = print
def print(*args, **kwargs):
    kwargs.setdefault('flush', True)
    _orig_print(*args, **kwargs)

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from googleapiclient.errors import HttpError

PACKAGE_NAME = 'com.auracast.weather'
KEY_FILE = os.path.join('fastlane', 'play-service-account.json')
METADATA_DIR = os.path.join('fastlane', 'metadata', 'android')

def upload_with_retry(fn, max_retries=3, delay=3):
    for attempt in range(1, max_retries + 1):
        try:
            return fn()
        except HttpError as e:
            if e.resp.status in [500, 502, 503, 504] and attempt < max_retries:
                print(f"    [Retry {attempt}/{max_retries}] transient error: {e}, waiting {delay}s...")
                time.sleep(delay)
                delay *= 2
            else:
                raise

def main():
    print(f"Loading service account key from {KEY_FILE}...")
    credentials = service_account.Credentials.from_service_account_file(
        KEY_FILE,
        scopes=['https://www.googleapis.com/auth/androidpublisher']
    )
    service = build('androidpublisher', 'v3', credentials=credentials)

    print(f"Creating edit for package {PACKAGE_NAME}...")
    edit = service.edits().insert(packageName=PACKAGE_NAME, body={}).execute()
    edit_id = edit['id']
    print(f"Created edit with ID: {edit_id}")

    # Set store details
    print("\n--- Setting Store Details ---")
    try:
        service.edits().details().patch(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            body={
                'defaultLanguage': 'en-US',
                'contactEmail': 'support@auracast.app',
                'contactWebsite': 'https://auracast-weather.web.app',
            }
        ).execute()
        print("Updated App details (defaultLanguage: en-US, support email, website).")
    except Exception as e:
        print(f"Details patch notice: {e}")

    # Update listings & images per locale
    locales = [d for d in os.listdir(METADATA_DIR) if os.path.isdir(os.path.join(METADATA_DIR, d))]
    # Put en-US first
    if 'en-US' in locales:
        locales.remove('en-US')
        locales.insert(0, 'en-US')

    for locale in locales:
        loc_path = os.path.join(METADATA_DIR, locale)
        title_path = os.path.join(loc_path, 'title.txt')
        short_path = os.path.join(loc_path, 'short_description.txt')
        full_path = os.path.join(loc_path, 'full_description.txt')
        video_path = os.path.join(loc_path, 'video.txt')

        if not (os.path.exists(title_path) and os.path.exists(short_path) and os.path.exists(full_path)):
            continue

        with open(title_path, 'r', encoding='utf-8') as f:
            title = f.read().strip()
        with open(short_path, 'r', encoding='utf-8') as f:
            short_desc = f.read().strip()
        with open(full_path, 'r', encoding='utf-8') as f:
            full_desc = f.read().strip()
        video = None
        if os.path.exists(video_path):
            with open(video_path, 'r', encoding='utf-8') as f:
                video = f.read().strip()

        body = {
            'title': title,
            'shortDescription': short_desc,
            'fullDescription': full_desc,
        }
        if video:
            body['video'] = video

        print(f"\n[{locale}] Updating listing: '{title}'...")
        upload_with_retry(lambda: service.edits().listings().update(
            packageName=PACKAGE_NAME,
            editId=edit_id,
            language=locale,
            body=body
        ).execute())
        print(f"[{locale}] ✓ Listing text updated successfully.")

        images_dir = os.path.join(loc_path, 'images')
        if not os.path.exists(images_dir):
            continue

        # 1. App Icon (512x512)
        icon_path = os.path.join(images_dir, 'icon.png')
        if os.path.exists(icon_path):
            try:
                upload_with_retry(lambda: service.edits().images().deleteall(
                    packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='icon'
                ).execute())
                media = MediaFileUpload(icon_path, mimetype='image/png')
                upload_with_retry(lambda: service.edits().images().upload(
                    packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='icon', media_body=media
                ).execute())
                print(f"[{locale}]   ✓ Uploaded 512x512 icon")
            except Exception as e:
                print(f"[{locale}]   Notice for icon: {e}")

        # 2. Feature Graphic (1024x500)
        fg_path = os.path.join(images_dir, 'featureGraphic.png')
        if os.path.exists(fg_path):
            try:
                upload_with_retry(lambda: service.edits().images().deleteall(
                    packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='featureGraphic'
                ).execute())
                media = MediaFileUpload(fg_path, mimetype='image/png')
                upload_with_retry(lambda: service.edits().images().upload(
                    packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='featureGraphic', media_body=media
                ).execute())
                print(f"[{locale}]   ✓ Uploaded featureGraphic")
            except Exception as e:
                print(f"[{locale}]   Notice for featureGraphic: {e}")

        # 3. Phone Screenshots (min 2)
        phone_dir = os.path.join(images_dir, 'phoneScreenshots')
        if os.path.exists(phone_dir):
            shots = sorted(glob.glob(os.path.join(phone_dir, '*.png')))
            if shots:
                try:
                    upload_with_retry(lambda: service.edits().images().deleteall(
                        packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='phoneScreenshots'
                    ).execute())
                    for s in shots:
                        media = MediaFileUpload(s, mimetype='image/png')
                        upload_with_retry(lambda: service.edits().images().upload(
                            packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='phoneScreenshots', media_body=media
                        ).execute())
                    print(f"[{locale}]   ✓ Uploaded {len(shots)} phone screenshots")
                except Exception as e:
                    print(f"[{locale}]   Notice for phone screenshots: {e}")

        # 4. 7-inch Screenshots
        seven_dir = os.path.join(images_dir, 'sevenInchScreenshots')
        if os.path.exists(seven_dir):
            shots = sorted(glob.glob(os.path.join(seven_dir, '*.png')))
            if shots:
                try:
                    upload_with_retry(lambda: service.edits().images().deleteall(
                        packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='sevenInchScreenshots'
                    ).execute())
                    for s in shots:
                        media = MediaFileUpload(s, mimetype='image/png')
                        upload_with_retry(lambda: service.edits().images().upload(
                            packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='sevenInchScreenshots', media_body=media
                        ).execute())
                    print(f"[{locale}]   ✓ Uploaded {len(shots)} 7-inch screenshots")
                except Exception as e:
                    print(f"[{locale}]   Notice for 7-inch screenshots: {e}")

        # 5. 10-inch Screenshots
        ten_dir = os.path.join(images_dir, 'tenInchScreenshots')
        if os.path.exists(ten_dir):
            shots = sorted(glob.glob(os.path.join(ten_dir, '*.png')))
            if shots:
                try:
                    upload_with_retry(lambda: service.edits().images().deleteall(
                        packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='tenInchScreenshots'
                    ).execute())
                    for s in shots:
                        media = MediaFileUpload(s, mimetype='image/png')
                        upload_with_retry(lambda: service.edits().images().upload(
                            packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='tenInchScreenshots', media_body=media
                        ).execute())
                    print(f"[{locale}]   ✓ Uploaded {len(shots)} 10-inch screenshots")
                except Exception as e:
                    print(f"[{locale}]   Notice for 10-inch screenshots: {e}")

    # Commit the edit
    print("\n=======================================================")
    print(f"Committing edit {edit_id} to Google Play Console...")
    commit_response = service.edits().commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(f"SUCCESS! Edit {edit_id} committed successfully!")
    print("All store listings, 512x512 icons, feature graphics, and tablet/phone screenshots are now live on Google Play Console!")
    print("=======================================================")

if __name__ == '__main__':
    main()
