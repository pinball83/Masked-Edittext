# Maintainers / Релиз

## Чеклист релиза

1. Выставить версию, которую публикуем (рекомендуется через Gradle property):
   - `./gradlew -PVERSION=2.x.y :masked-edittext:assembleRelease`
2. Прогнать проверки:
   - `./gradlew lint test assembleRelease`
3. (Опционально) Проверить как потребитель:
   - `./gradlew :masked-edittext:publishToMavenLocal`
4. Опубликовать в Maven Central:
   - `./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository`
5. Проверить в Sonatype Central:
   - https://central.sonatype.com/artifact/io.github.pinball83/masked-edittext/overview

## Учётные данные (Sonatype Central)

Для публикации нужен Sonatype *User Token* (не пароль от аккаунта).

Задать можно так:
- `~/.gradle/gradle.properties`:
  - `sonatypeUsername=...`
  - `sonatypePassword=...`
- Environment variables:
  - `OSSRH_USERNAME` / `OSSRH_PASSWORD`
  - (also accepted) `SONATYPE_USERNAME` / `SONATYPE_PASSWORD`

## Подпись артефактов (PGP)

Артефакты подписываются in-memory PGP ключом. Нужно задать:
- `signingKeyId` (last 8 hex chars are used)
- `signingKey` (ASCII-armored key, or base64-encoded ASCII-armored key)
- `signingPassword`

Куда положить значения:
- `~/.gradle/gradle.properties`, или
- локальный `signing.properties` в корне репозитория (gitignored), или
- локальный `masked-edittext/signing.properties` (gitignored)

Шаблон: `signing.properties.example` (скопируйте в `signing.properties` и заполните).

## Важно про безопасность

Никогда не коммитьте `signing.properties` (и любые токены/приватные ключи). Если секреты когда-либо утекли, сразу ротируйте их (Sonatype user token + PGP key).
