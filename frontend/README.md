# IncidentHub frontend

Фронтенд IncidentHub: React, Vite и TypeScript. Интерфейс рассчитан на десктоп
и использует только тёмную тему.

## Локальный запуск

Запустите backend из корня репозитория в профиле `demo`, затем фронтенд:

```powershell
npm install
npm run dev
```

Vite проксирует `/api` на `http://localhost:8080`. Если API доступен по другому
адресу, задайте `VITE_API_BASE_URL` в локальном `.env`.

Демо-пользователи и общий пароль `demo1234`:

| Роль       | Email                            |
| ---------- | -------------------------------- |
| `REPORTER` | `anna.ivanova@incidenthub.demo`  |
| `ENGINEER` | `boris.petrov@incidenthub.demo`  |
| `ADMIN`    | `olga.smirnova@incidenthub.demo` |

## API-клиент

Orval генерирует Fetch-клиент и модели из зафиксированного снимка
`openapi/incident-hub.json`:

```powershell
npm run api:generate
```

Чтобы обновить снимок, сначала запустите backend, затем выполните:

```powershell
npm run api:update
```

Адрес спецификации можно переопределить переменной `OPENAPI_URL`. В CI backend
и PostgreSQL для frontend job не нужны: генерация выполняется из закоммиченного
JSON-файла. Команда `npm run api:check` повторяет генерацию и проверяет, что
сгенерированный код не изменился.

Общий transport в `src/api/http.ts` добавляет Bearer-токен и очищает сессию при 401. DTO и функции конкретных endpoint находятся только в `src/api/generated`
и вручную не редактируются.

## Архитектурные решения

Стили написаны на CSS Modules. Палитра, типографика, размеры, интервалы и
радиусы вынесены в `src/styles/tokens.css`, поэтому визуальные значения из
прототипа переносятся напрямую, без дополнительного слоя Tailwind utility.

JWT хранится в `localStorage`, чтобы сессия переживала обновление страницы.
Такой вариант прост для учебного проекта, но доступный странице JavaScript при
XSS-код также сможет прочитать токен. Риск ограничен часовым сроком жизни токена,
отказом от вставки непроверенного HTML и очисткой токена при 401. Профиль, имя и
роль загружаются через `/api/v1/users/me`; JWT не является источником данных UI.

Русские переводы находятся в одном файле `src/i18n/locales/ru.ts`. Для
добавления английского достаточно скопировать его в `en.ts`, сохранить структуру
ключей и зарегистрировать ресурс в `src/i18n/index.ts`.

## Проверки

```powershell
npm run lint
npm run format:check
npm run test
npm run build
```
