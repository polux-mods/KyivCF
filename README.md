# Kyiv Transport Clone — Kotlin / Jetpack Compose

Готовий Android Studio проєкт на Kotlin + Jetpack Compose. Екран зроблений під вигляд додатку зі скріншота:

- синій головний екран;
- верхні погодні/пошукові елементи;
- карусель сервісів;
- функціональна тільки плитка **«Оплата проїзду»**;
- інші плитки зверху залишені муляжем;
- нижній список сповіщень;
- реальний стан повітряної тривоги для **м. Київ** через `https://ubilling.net.ua/aerialalerts/`.

## Що змінено для GitHub

Проєкт можна одразу заливати в репозиторій:

- додано `.gitignore`;
- прибрано потребу в API-токені;
- немає секретів у коді;
- додано `.github/workflows/android.yml` для перевірки збірки на GitHub Actions;
- `local.properties` не потрібен для API і не має потрапляти в GitHub.

## Як запустити

1. Відкрий папку `KyivTransportClone` в Android Studio.
2. Дочекайся Gradle Sync.
3. Запусти додаток на телефоні або емуляторі.

## API тривог

Додаток кожні 60 секунд робить запит до:

```text
https://ubilling.net.ua/aerialalerts/
```

Для Києва використовується ключ:

```text
м. Київ
```

Приклад потрібного блока з відповіді:

```json
"м. Київ": {
  "alertnow": false,
  "changed": "2026-06-26 09:59:08"
}
```

Якщо `alertnow = true`, у списку сповіщень показується червоне повідомлення **«повітряна тривога!»**.

Якщо `alertnow = false`, показується зелене повідомлення **«відбій тривоги»**.

## Як залити на GitHub

```bash
git init
git add .
git commit -m "Initial Android app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/kyiv-transport-clone.git
git push -u origin main
```

## Примітка

`ubilling.net.ua/aerialalerts/` — стороннє джерело. Якщо структура JSON на сайті зміниться, парсер у додатку також треба буде оновити.
