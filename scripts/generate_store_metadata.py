import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

locales = {
    'es-419': {
        'title': 'AuraCast Clima',
        'short': 'Tu clima, narrado — podcast IA en el dispositivo y radar en vivo sin cuenta.',
        'desc': '''Tu clima, narrado — totalmente en tu dispositivo.

AuraCast Clima convierte tu pronóstico diario en un informe hablado natural, generado directamente en tu teléfono con IA Gemma y voz neuronal real. Sin cuentas y sin llamadas a la nube para generar el reporte.

• Pronóstico actual, por horas (48h) y 5 días (Open-Meteo y NWS) con íconos animados Meteocons.
• Podcast del clima con IA — Gemma se ejecuta en el dispositivo mediante LiteRT-LM, narrado con voz neuronal ultrarrealista.
• Radar de lluvia animado — Capas de RainViewer y NWS sobre mapas libres con controles de reproducción interactivos.
• Traducción sin conexión a 59 idiomas mediante Google ML Kit — descarga una vez y traduce todo sin internet.
• Alertas de clima severo y resumen matutino diario.
• Temas claro y oscuro Material 3, widgets de pantalla de inicio, 100% privado y sin registro.

Privacidad ante todo:
Tu reporte de IA nunca sale de tu teléfono. Diseñado en Kotlin y Jetpack Compose para celulares y tablets.

Soporte: support@auracast.app • Web: https://auracast-weather.web.app'''
    },
    'es-ES': {
        'title': 'AuraCast Clima',
        'short': 'Tu tiempo, narrado — podcast IA en el dispositivo y radar en directo.',
        'desc': '''Tu tiempo, narrado — totalmente en tu dispositivo.

AuraCast Clima convierte tu previsión diaria en un informe hablado natural, generado directamente en tu móvil con IA Gemma y voz neuronal real. Sin cuentas y sin llamadas a la nube para generar el reporte.

• Previsión actual, por horas (48h) y 5 días con iconos animados Meteocons.
• Podcast meteorológico con IA — Gemma se ejecuta en local mediante LiteRT-LM, narrado con voz neuronal ultrarrealista.
• Radar de precipitación animado — Capas de RainViewer sobre mapas libres con controles interactivos.
• Traducción sin conexión a 59 idiomas mediante Google ML Kit — descarga una vez y traduce todo sin conexión.
• Alertas meteorológicas y resumen matutino diario.
• Temas claro y oscuro Material 3, widgets para tu pantalla de inicio, 100% privado y sin registro.

Privacidad ante todo:
Tu informe de IA nunca sale de tu móvil. Diseñado en Kotlin y Jetpack Compose para teléfonos y tablets.

Soporte: support@auracast.app • Web: https://auracast-weather.web.app'''
    },
    'fr-FR': {
        'title': 'Météo AuraCast',
        'short': "Votre météo contée par l'IA sur votre appareil + radar en direct sans compte.",
        'desc': '''Votre météo, racontée — entièrement sur votre appareil.

AuraCast Météo transforme vos prévisions quotidiennes en un briefing vocal naturel, généré directement sur votre téléphone avec l'IA Gemma et une voix neuronale réaliste. Sans compte requis et sans transfert cloud pour générer le rapport.

• Prévisions actuelles, heure par heure (48h) et sur 5 jours avec de superbes icônes Meteocons animées.
• Podcast météo IA — Modèle Gemma exécuté sur l'appareil via LiteRT-LM avec synthèse vocale neuronale fluide.
• Radar de précipitations animé — Cartes RainViewer interactives avec lecture et défilement fluide.
• Traduction hors ligne en 59 langues via Google ML Kit — téléchargez une fois, profitez-en partout hors ligne.
• Alertes météo et notifications de briefing quotidien.
• Design moderne Material 3 (thèmes clair/sombre), widgets pour l'écran d'accueil, sans compte.

Confidentialité garantie :
Vos données météo et briefings restent strictement sur votre appareil. Développé nativement en Kotlin et Jetpack Compose.

Support : support@auracast.app • Site web : https://auracast-weather.web.app'''
    },
    'de-DE': {
        'title': 'AuraCast Wetter',
        'short': 'Dein Wetter als KI-Podcast auf dem Gerät + Live-Radar ohne Konto.',
        'desc': '''Dein Wetter, erzählt — komplett auf deinem Gerät.

AuraCast Wetter verwandelt deine tägliche Vorhersage in einen lebendigen Sprachbericht, direkt auf deinem Smartphone generiert mit Gemma-KI und natürlicher Sprachausgabe. Kein Konto nötig, keine Cloud-Anfragen für deinen Bericht.

• Aktuelle Bedingungen, 48-Stunden- und 5-Tage-Vorhersage mit animierten Meteocons-Symbolen.
• KI-Wetter-Podcast — Gemma läuft lokal auf dem Gerät via LiteRT-LM mit natürlicher Sprachausgabe.
• Animiertes Niederschlagsradar — RainViewer-Echtzeitkarten mit interaktiver Wiedergabe.
• Offline-Übersetzung in 59 Sprachen über Google ML Kit — einmal laden, dauerhaft offline nutzen.
• Unwetterwarnungen und tägliche Briefing-Benachrichtigungen.
• Material 3 Design (Hell/Dunkel), praktische Homescreen-Widgets, ohne Registrierung.

Datenschutz an erster Stelle:
Deine Berichte verlassen niemals dein Smartphone. Nativ entwickelt mit Kotlin und Jetpack Compose.

Support: support@auracast.app • Website: https://auracast-weather.web.app'''
    },
    'pt-BR': {
        'title': 'AuraCast Clima',
        'short': 'Seu clima narrado por IA no dispositivo + radar ao vivo sem conta.',
        'desc': '''Seu clima, narrado — totalmente no seu celular.

O AuraCast Clima transforma a sua previsão do tempo em um briefing falado natural, gerado direto no seu smartphone com IA Gemma e voz neural realista. Sem necessidade de conta e sem envio para a nuvem.

• Previsão atual, hora a hora (48h) e 5 dias com ícones animados Meteocons.
• Podcast meteorológico por IA — Gemma roda no dispositivo via LiteRT-LM com síntese de voz neural de alta qualidade.
• Radar de chuva animado — Mapas RainViewer em tempo real com reprodução e linha do tempo interativa.
• Tradução offline para 59 idiomas via Google ML Kit — baixe uma vez e use sem gastar internet.
• Alertas de tempestades e notificações de resumo matinal diário.
• Visual Material 3 elegante com temas claro e escuro, widgets de tela inicial, sem cadastro.

Privacidade em primeiro lugar:
Seu relatório nunca sai do seu aparelho. Construído nativamente em Kotlin e Jetpack Compose.

Suporte: support@auracast.app • Site: https://auracast-weather.web.app'''
    },
    'ja-JP': {
        'title': 'AuraCast 天気予報',
        'short': '端末内AIによる音声天気予報ポッドキャスト＋雨雲レーダー。登録不要。',
        'desc': '''あなたの天気、端末内で音声ナレーション。

AuraCast 天気予報は、スマホ本体のオンデバイスAI（Gemma）と高音質ニューラル音声で、今日の天気をまるでラジオ番組のように読み上げます。アカウント登録不要、レポート生成にクラウド通信は不要です。

• 現在の天気、48時間毎時予報、5日間の週間天気（美しいMeteoconsアニメーションアイコン対応）
• AI天気ポッドキャスト — LiteRT-LMで端末上で直接Gemmaが動作し、自然な音声でレポートを生成
• アニメーション雨雲レーダー — RainViewerの最新レーダーをインタラクティブに再生・確認
• Google ML Kitによる59言語オフライン翻訳 — パックを一度ダウンロードすれば完全に通信なしで翻訳
• 気象警報アラート＆毎日の朝刊ブリーフィング通知
• Material 3対応（ライト／ダークテーマ）、ホーム画面ウィジェット対応、登録不要ですぐ利用可能

プライバシー保護：
AI生成レポートが端末外に送信されることは一切ありません。KotlinとJetpack Composeによる完全ネイティブ設計。

サポート: support@auracast.app • 公式サイト: https://auracast-weather.web.app'''
    },
    'ko-KR': {
        'title': 'AuraCast 날씨',
        'short': '온디바이스 AI 음성 날씨 브리핑과 실시간 레이더. 계정 불필요.',
        'desc': '''당신만을 위한 음성 날씨 브리핑 — 완전히 온디바이스로 작동합니다.

AuraCast 날씨는 스마트폰 내에서 직접 구동되는 온디바이스 AI(Gemma)와 자연스러운 뉴럴 음성을 통해 매일의 날씨를 라디오 브리핑처럼 읽어드립니다. 계정 가입이 필요 없으며 리포트 생성에 클라우드 서버를 거치지 않습니다.

• 현재 날씨, 48시간 시간별 예보, 5일간 주간 예보 및 감각적인 Meteocons 애니메이션 아이콘
• AI 날씨 팟캐스트 — LiteRT-LM을 통해 기기에서 직접 Gemma 구동, 선호하는 뉴럴 음성 선택 가능
• 실시간 강수 레이더 애니메이션 — RainViewer 위성 타일과 편리한 재생/스크롤 컨트롤
• Google ML Kit 기반 59개국 언어 오프라인 번역 — 한 번 다운로드하면 인터넷 없이 모든 화면 번역
• 기상 특보 알림 및 매일 아침 데일리 브리핑
• 깔끔한 Material 3 라이트/다크 테마 및 홈 화면 위젯 제공

철저한 프라이버시:
AI 브리핑 데이터는 결코 기기 외부로 전송되지 않습니다. Kotlin과 Jetpack Compose로 제작된 순수 네이티브 앱입니다.

문의: support@auracast.app • 웹사이트: https://auracast-weather.web.app'''
    },
    'zh-CN': {
        'title': 'AuraCast 天气',
        'short': '端侧AI语音播报天气预报与实时雷达动图，无需注册账号。',
        'desc': '''你的专属天气播报 — 纯端侧离线智能运行。

AuraCast 天气将每日预报转化为自然流畅的语音简报。基于手机本地 Gemma AI 与神经拟真语音技术，完全在本地手机生成，无需注册账号，报告生成无需经过云端服务器。

• 实时天气、48小时逐时预报与5天天气展望，配备精美动效 Meteocons 图标。
• AI天气播客 — 通过 LiteRT-LM 在设备端原生运行 Gemma，清晰生动的语音朗读。
• 动态降水雷达 — RainViewer 实时雷达动图，支持流畅播放与拖动预览。
• 59种语言离线翻译 — 借助 Google ML Kit，只需一次下载即可全离线翻译所有界面。
• 极端天气预警通知与每日晨间简报。
• Material 3 现代美学（深色/浅色主题），桌面小组件支持，免账号即开即用。

隐私第一：
AI 报告永远留在你的手机上，绝不上报云端。采用 Kotlin 和 Jetpack Compose 纯原生打造。

技术支持：support@auracast.app • 官网：https://auracast-weather.web.app'''
    },
    'hi-IN': {
        'title': 'AuraCast मौसम',
        'short': 'ऑन-डिवाइस AI वॉइस पॉडकास्ट और लाइव रडार। किसी खाते की आवश्यकता नहीं।',
        'desc': '''आपका मौसम, अब आपकी आवाज़ में — पूरी तरह से आपके फोन पर।

AuraCast मौसम आपके दैनिक पूर्वानुमान को एक प्राकृतिक बोले जाने वाले पॉडकास्ट में बदल देता है, जो सीधे आपके फ़ोन पर Gemma AI और न्यूरल आवाज़ के साथ तैयार होता है। किसी खाते की आवश्यकता नहीं है।

• वर्तमान मौसम, 48-घंटे का प्रति घंटा विवरण और 5-दिन का पूर्वानुमान (एनिमेटेड Meteocons आइकन के साथ)।
• AI मौसम पॉडकास्ट — LiteRT-LM के जरिए फोन पर सीधे Gemma मॉडल चलता है।
• एनिमेटed रडार — RainViewer मैप्स के साथ लाइव बारिश का रडार और प्लेबैक।
• Google ML Kit के माध्यम से 59 भाषाओं में ऑफलाइन अनुवाद — एक बार डाउनलोड करें और बिना इंटरनेट उपयोग करें।
• गंभीर मौसम अलर्ट और दैनिक ब्रीफिंग सूचनाएं।
• Material 3 लाइट और डार्क थीम, होमस्क्रीन विजेट्स।

गोपनीयता प्राथमिकता:
आपकी AI रिपोर्ट कभी आपके फोन से बाहर नहीं जाती। पूरी तरह से नेटिव Kotlin और Jetpack Compose में निर्मित।

ईमेल: support@auracast.app • वेबसाइट: https://auracast-weather.web.app'''
    }
}

video_url = 'https://youtu.be/33B6BICsm7c\n'

for loc, data in locales.items():
    loc_dir = os.path.join('fastlane', 'metadata', 'android', loc)
    os.makedirs(loc_dir, exist_ok=True)
    with open(os.path.join(loc_dir, 'title.txt'), 'w', encoding='utf-8') as f:
        f.write(data['title'] + '\n')
    with open(os.path.join(loc_dir, 'short_description.txt'), 'w', encoding='utf-8') as f:
        f.write(data['short'] + '\n')
    with open(os.path.join(loc_dir, 'full_description.txt'), 'w', encoding='utf-8') as f:
        f.write(data['desc'] + '\n')
    with open(os.path.join(loc_dir, 'video.txt'), 'w', encoding='utf-8') as f:
        f.write(video_url)
    print(f'Wrote metadata for {loc}: title={data["title"]} ({len(data["title"])} chars), short={len(data["short"])} chars')
