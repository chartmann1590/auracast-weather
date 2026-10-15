import os
import sys
import glob
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

_orig_print = print
def print(*args, **kwargs):
    kwargs.setdefault('flush', True)
    _orig_print(*args, **kwargs)

PACKAGE_NAME = 'com.auracast.weather'
KEY_FILE = os.path.join('fastlane', 'play-service-account.json')
METADATA_DIR = os.path.join('fastlane', 'metadata', 'android')
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

    print(f"Creating edit for package {PACKAGE_NAME}...")
    try:
        edit_request = service.edits().insert(packageName=PACKAGE_NAME, body={})
        edit = edit_request.execute()
        edit_id = edit['id']
        print(f"Created edit with ID: {edit_id}")
    except Exception as e:
        print(f"Failed to create edit: {e}")
        print("\nNote: Make sure this app exists in Google Play Console and the service account 'play-publisher@auracast-weather.iam.gserviceaccount.com' has been invited under Users & permissions with Release Manager/Admin permissions.")
        sys.exit(1)

    # 1. Update listings
    print("\n--- Updating Store Listings ---")
    locales = [d for d in os.listdir(METADATA_DIR) if os.path.isdir(os.path.join(METADATA_DIR, d))]
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

        try:
            service.edits().listings().update(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                language=locale,
                body=body
            ).execute()
            print(f"Updated listing for locale: {locale} (Title: {title})")
        except Exception as e:
            print(f"Warning: Failed to update listing for {locale}: {e}")

        # 2. Upload images for this locale
        images_dir = os.path.join(loc_path, 'images')
        if os.path.exists(images_dir):
            feature_graphic = os.path.join(images_dir, 'featureGraphic.png')
            if os.path.exists(feature_graphic):
                try:
                    # Clear existing feature graphic first
                    service.edits().images().deleteall(
                        packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='featureGraphic'
                    ).execute()
                    media = MediaFileUpload(feature_graphic, mimetype='image/png')
                    service.edits().images().upload(
                        packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='featureGraphic', media_body=media
                    ).execute()
                    print(f"  Uploaded featureGraphic for {locale}")
                except Exception as e:
                    print(f"  Warning: featureGraphic upload for {locale}: {e}")

            # Phone screenshots
            phone_dir = os.path.join(images_dir, 'phoneScreenshots')
            if os.path.exists(phone_dir):
                phone_shots = sorted(glob.glob(os.path.join(phone_dir, '*.png')))
                if phone_shots:
                    try:
                        service.edits().images().deleteall(
                            packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='phoneScreenshots'
                        ).execute()
                        for shot in phone_shots:
                            media = MediaFileUpload(shot, mimetype='image/png')
                            service.edits().images().upload(
                                packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='phoneScreenshots', media_body=media
                            ).execute()
                        print(f"  Uploaded {len(phone_shots)} phone screenshots for {locale}")
                    except Exception as e:
                        print(f"  Warning: phone screenshots upload for {locale}: {e}")

            # 7-inch screenshots
            seven_dir = os.path.join(images_dir, 'sevenInchScreenshots')
            if os.path.exists(seven_dir):
                seven_shots = sorted(glob.glob(os.path.join(seven_dir, '*.png')))
                if seven_shots:
                    try:
                        service.edits().images().deleteall(
                            packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='sevenInchScreenshots'
                        ).execute()
                        for shot in seven_shots:
                            media = MediaFileUpload(shot, mimetype='image/png')
                            service.edits().images().upload(
                                packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='sevenInchScreenshots', media_body=media
                            ).execute()
                        print(f"  Uploaded {len(seven_shots)} 7-inch screenshots for {locale}")
                    except Exception as e:
                        print(f"  Warning: 7-inch screenshots upload for {locale}: {e}")

            # 10-inch screenshots
            ten_dir = os.path.join(images_dir, 'tenInchScreenshots')
            if os.path.exists(ten_dir):
                ten_shots = sorted(glob.glob(os.path.join(ten_dir, '*.png')))
                if ten_shots:
                    try:
                        service.edits().images().deleteall(
                            packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='tenInchScreenshots'
                        ).execute()
                        for shot in ten_shots:
                            media = MediaFileUpload(shot, mimetype='image/png')
                            service.edits().images().upload(
                                packageName=PACKAGE_NAME, editId=edit_id, language=locale, imageType='tenInchScreenshots', media_body=media
                            ).execute()
                        print(f"  Uploaded {len(ten_shots)} 10-inch screenshots for {locale}")
                    except Exception as e:
                        print(f"  Warning: 10-inch screenshots upload for {locale}: {e}")

    # 3. Upload AAB bundle
    version_code = None
    if os.path.exists(AAB_PATH):
        print(f"\n--- Uploading Release AAB: {AAB_PATH} ---")
        try:
            media = MediaFileUpload(AAB_PATH, mimetype='application/octet-stream', resumable=True)
            bundle_response = service.edits().bundles().upload(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                media_body=media,
                ackBundleInstallationWarning=True
            ).execute()
            version_code = bundle_response['versionCode']
            print(f"Successfully uploaded AAB bundle with versionCode: {version_code}")
        except Exception as e:
            print(f"Warning: AAB upload error: {e}")
    else:
        print(f"\nAAB file not found at {AAB_PATH}")

    # 4. Assign to production track as draft
    if version_code is not None:
        print(f"\n--- Assigning to Production track as DRAFT ---")
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
        try:
            service.edits().tracks().update(
                packageName=PACKAGE_NAME,
                editId=edit_id,
                track='production',
                body=track_body
            ).execute()
            print("Successfully assigned release to production track as DRAFT.")
        except Exception as e:
            print(f"Warning: Track assignment error: {e}")

    # 5. Commit edit
    print("\n--- Committing Edit to Google Play ---")
    try:
        commit_res = service.edits().commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
        print(f"SUCCESS! Play Store edit {edit_id} committed successfully!")
    except Exception as e:
        print(f"Commit failed: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
