import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

from google.oauth2 import service_account
from googleapiclient.discovery import build

KEY_FILE = os.path.join('fastlane', 'play-service-account.json')
PACKAGE_NAME = 'com.auracast.weather'

def main():
    creds = service_account.Credentials.from_service_account_file(
        KEY_FILE,
        scopes=['https://www.googleapis.com/auth/androidpublisher']
    )
    service = build('androidpublisher', 'v3', credentials=creds)
    edit = service.edits().insert(packageName=PACKAGE_NAME, body={}).execute()
    edit_id = edit['id']

    details = service.edits().details().get(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(f"Default Language: {details.get('defaultLanguage')}")
    print(f"Contact Email: {details.get('contactEmail')}")
    print(f"Contact Website: {details.get('contactWebsite')}")

    listings = service.edits().listings().list(packageName=PACKAGE_NAME, editId=edit_id).execute()
    active_listings = listings.get('listings', [])
    print(f"\nTotal Active Listings in Google Play: {len(active_listings)}")
    for l in active_listings:
        print(f"  [{l['language']}] Title: '{l['title']}' | Video: {l.get('video', 'N/A')}")

    tracks = service.edits().tracks().list(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print("\nTracks:")
    for t in tracks.get('tracks', []):
        releases = t.get('releases', [])
        if releases:
            print(f"  Track: {t['track']} | Releases: {len(releases)}")
            for r in releases:
                print(f"    Release: {r.get('name')} | Status: {r.get('status')} | VersionCodes: {r.get('versionCodes')}")

if __name__ == '__main__':
    main()
