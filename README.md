<img src="https://github.com/MCXTeam/UnicoreConnect/blob/main/unicoreconnect.png?raw=true?v=2" />

# UnicoreConnect ![Kotlin](https://img.shields.io/badge/-Kotlin-05122A?style=flat&logo=Kotlin&logoColor=FFA518)&nbsp;
[![Build Status](https://github.com/MCXTeam/UnicoreConnect/actions/workflows/build.yml/badge.svg)](https://github.com/MCXTeam/UnicoreConnect/actions)

> Связывает игровой сервер с UnicoreCMS: экономика, донат-группы и права, склад покупок, баны и статистика онлайна.

## Поддерживаемые платформы

| Файл | Платформа | Версии игры | Java |
| --- | --- | --- | --- |
| `UnicoreConnect-bukkit` | Spigot, Paper и их форки, гибриды (Thermos, Mohist, Magma) | 1.7.10 — 1.18.1 | 8+ |
| `UnicoreConnect-forge-1.7.10` | Forge | 1.7.10 | 8 |
| `UnicoreConnect-forge-1.12.2` | Forge | 1.12.2 | 8 |
| `UnicoreConnect-forge-1.19.2` | Forge | 1.19.2 | 17 |
| `UnicoreConnect-forge-1.20.1` | Forge | 1.20.1 | 17 |
| `UnicoreConnect-neoforge-1.21.1` | NeoForge | 1.21.1 | 21 |

Файлы с суффиксами `-slim` и `-dev` — промежуточные сборки, на сервер ставится файл без суффикса.

## Адаптеры

Ядро не знает ни об одном стороннем плагине: оно обращается к адаптерам, которые лежат в `adapters/`.
Адаптер включается сам, если нужный плагин найден на сервере.

### Права и группы

| Адаптер | Где работает | Что делает |
| --- | --- | --- |
| `luckperms` | Bukkit, Forge с портом LuckPerms | Выдаёт группы и права через API, понимает срок действия |
| `forgeessentials` | Forge 1.7.10 и 1.12.2 | Выдаёт группы и права через API ForgeEssentials |
| `luxinfine` | Forge 1.7.10 с LuxinfineHelper | Выдаёт группы и права через провайдер прав LuxinfineHelper, понимает срок действия |
| `commands` | везде | Выполняет консольные команды из конфигурации |

Адаптер выбирается настройкой `permissions.adapter`: `auto` — взять первый доступный,
либо имя адаптера из таблицы.

Адаптер `commands` рассчитан на плагины без публичного API: в шаблонах команд доступны
`{user.uuid}`, `{user.username}`, `{group.ingame_id}`, `{permission.node}` и `{period.duration}`.
По умолчанию шаблоны написаны под LuckPerms.

ForgeEssentials не хранит срок действия прав: временные группы снимаются сайтом — при следующем
входе игрока набор групп и прав приводится к тому, что записано в UnicoreCMS.

### Экономика и баны

| Адаптер | Где работает | Что делает |
| --- | --- | --- |
| `vault` | Bukkit | Отдаёт баланс сайта другим плагинам через Vault |
| `banmanager` | Bukkit | Двусторонняя синхронизация банов с сайтом |
| `luxinfine` (экономика) | Forge 1.7.10 с LuxinfineHelper | Регистрирует баланс сайта как интеграцию `unicorecms` |
| `luxinfine` (баны) | Forge 1.7.10 с LuxinfineHelper | Баны через провайдер наказаний LuxinfineHelper |

На Forge баны идут через ванильный банлист сервера, а экономика работает сама по себе —
баланс хранится на сайте.

### LuxinfineHelper

[LuxinfineHelper](https://github.com/LuxinfineTeam/PublicLibraries/tree/main/LuxinfineHelper) —
слой интеграций для 1.7.10: за ним могут стоять LuxinfinePermissions, LuxinfineEconomy, Vault,
LuckPerms, ванильные баны и другие провайдеры. Мод для 1.7.10 работает с ним напрямую, поэтому
один адаптер закрывает сразу всё, что настроено в `config/Luxinfine/LFHelperIntegrations.json`.

- **Права** берутся, если провайдер умеет выдавать группы или права. Провайдер `OpBased` умеет
  только читать, поэтому на нём выдача уходит в адаптер `commands`, а проверки прав команд
  всё равно идут через LuxinfineHelper.
- **Баны** идут через провайдер наказаний, если он поддерживает тип `BAN`.
- **Экономика** регистрируется как интеграция с именем `unicorecms`. Чтобы мод на LuxinfineHelper
  брал баланс с сайта, выберите её в его настройке провайдера экономики. Проверить список —
  команда `/integrations`.

## Установка и настройка

1. [Создайте API-ключ](https://unicorecms.ru/docs/admin/api-keys#создание-api-ключа) с правом `kernel.unicore.connect`.
2. Положите файл в папку `plugins` (Bukkit) или `mods` (Forge, NeoForge).
3. Запустите сервер — конфигурация создастся сама, впишите в неё адрес сайта и ключ.

Bukkit читает `plugins/UnicoreConnect/config.yml`:

```yaml
server: id сервера
api:
  url: Адрес UnicoreCMS-сервера
  key: API-ключ
```

Forge и NeoForge читают `config/unicoreconnect.json`:

```json
{
  "server": "id сервера",
  "apiUrl": "Адрес UnicoreCMS-сервера",
  "apiKey": "API-ключ",
  "modules": {
    "money": true,
    "playtime": true,
    "showcase": true,
    "donate": true,
    "bans": true
  },
  "permissions": {
    "adapter": "auto"
  }
}
```

Любой модуль можно выключить в разделе `modules` — команды и обработчики выключенного модуля
не регистрируются.

## Сборка

#### Что нужно
* Git
* JDK 21 — для Bukkit и Forge 1.19.2 и новее
* JDK 25 и JDK 8 — для Forge 1.7.10 и 1.12.2: Gradle 9 запускается на 25, мод компилируется под 8

#### Bukkit и Forge 1.19.2 и новее

```sh
git clone https://github.com/MCXTeam/UnicoreConnect.git
cd UnicoreConnect/
./gradlew build
cd platforms/forge-1.20.1 && ../../gradlew build
```

Файл плагина — `bukkit/build/libs/UnicoreConnect-bukkit-<версия>.jar`.

#### Forge 1.7.10 и 1.12.2

Старые версии собираются отдельным Gradle, поэтому ядро сначала кладётся в mavenLocal:

```sh
./gradlew publishToMavenLocal
cd platforms/forge-1.12.2 && ./gradlew build
```

Собранные файлы лежат в `build/libs` своего модуля.

## Релизы

Версия хранится в одном месте — `gradle.properties` в корне репозитория. Меняете её и пушите
в `main` — CI собирает все платформы, ставит тег `v<версия>` и публикует релиз с файлами.
Пуш без смены версии релиз не создаёт: тег уже занят, шаг публикации пропускается.

## Команды и права

### Экономика
| Команды | Пермишен | Описание |
| --- |  --- |  --- |
| /money | unicoreconnect.command.money | Баланс внутриигровой валюты |
| /money top | unicoreconnect.command.money.top | Топ 10 богачей сервера |
| /money pay \[player\] \[amount\] | unicoreconnect.command.money.pay | Перевести монеты игроку |
**Алиасы:** /bal, /balance

### PlayTime
Команды | Пермишен | Описание |
| --- |  --- |  --- |
| /playtime | unicoreconnect.command.playtime | Время проведённое на сервере |
| /playtime top | unicoreconnect.command.playtime.top | Топ 10 по времени онлайн сервера |
**Алиасы:** /pt

### Склад и магазин
Команды | Пермишен | Описание |
| --- |  --- |  --- |
| /cart list | unicoreconnect.command.showcase.list | Список товаров на складе |
| /cart give \[id\] | unicoreconnect.command.showcase.give | Выдать товар со склада |
| /cart all | unicoreconnect.command.showcase.all | Выдать все товары со склада |
**Алиасы:** /showcase

### Администрирование
Команды | Пермишен | Описание |
| --- |  --- |  --- |
| /cart create \[price\] \[name\] | unicoreconnect.admin.showcase.create | Добавить предмет, находящийся в руке в магазин |
| /uc sync или /unicoreconnect sync | unicoreconnect.admin.sync | Переподключится к UnicoreServer и заного синхронизировать донат-группы и донат-права |

На Forge 1.12.2 права команд регистрируются в `PermissionAPI`, поэтому их видит ForgeEssentials
и любой другой обработчик прав. Если обработчика нет, команды доступны игрокам с уровнем оператора.

## Тексты сообщений

Forge и NeoForge берут сообщения о выдаче донат-групп и прав из раздела `messages` файла
`config/unicoreconnect.json`:

```json
{
  "messages": {
    "giveGroup": "Донат-группа «{name}» выдана",
    "takeGroup": "Донат-группа «{name}» снята",
    "givePermission": "Донат-право «{name}» выдано",
    "takePermission": "Донат-право «{name}» снято"
  }
}
```

Bukkit хранит тексты в файле **acf-unicoreconnect_ru.properties** внутри архива плагина:

```properties
unicoreconnect.command_money=Ваш баланс на сервере {server}: <c2>{money}</c2>
unicoreconnect.command_money_pay=Перевод игроку <c2>{target}</c2> успешно совершён. Текущий баланс: <c2>{money}</c2>
unicoreconnect.command_money_pay_target=Вам поступил перевод <c2>{amount}</c2> от <c2>{player}</c2>
unicoreconnect.command_money_pay_fail=При переводе произошла ошибка, возможно на балансе недостаточно денег!
unicoreconnect.command_money_top=<c2>Топ-богачей {server}:</c2>\n{rows}

unicoreconnect.command_playtime=Время проведённое на сервере {server}: <c2>{time}</c2>
unicoreconnect.command_playtime_top=<c2>Топ-онлайн {server}:</c2>\n{rows}

unicoreconnect.command_showcase_create=<c2>{id}</c2> успешно добавлен в веб-магазин
unicoreconnect.command_showcase=Выдано <c2>{amount}</c2> предметов со склада
unicoreconnect.command_showcase_list=<c2>Предметы на вашем складе:</c2>\n{items}
unicoreconnect.command_showcase_all_fail=Ваш склад пуст!
unicoreconnect.command_showcase_give_fail=Предмет #<c2>{id}</c2> не найден на вашем складе!

unicoreconnect.event_give_group=Поздравляем вас с покупкой донат-группы - <c2>{name}</c2>!
unicoreconnect.event_take_group=С вас была снята донат-группа - <c2>{name}</c2>!
unicoreconnect.event_give_permission=Поздравляем вас с покупкой донат-права - <c2>{name}</c2>!
unicoreconnect.event_take_permission=С вас была снята донат-права - <c2>{name}</c2>!
```
