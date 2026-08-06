<img src="https://github.com/MCXTeam/UnicoreConnect/blob/main/unicoreconnect.png?raw=true?v=2" />

# UnicoreConnect ![Kotlin](https://img.shields.io/badge/-Kotlin-05122A?style=flat&logo=Kotlin&logoColor=FFA518)&nbsp;
[![Build Status](https://github.com/MCXTeam/UnicoreConnect/actions/workflows/gradle.yml/badge.svg)](https://github.com/MCXTeam/UnicoreConnect/actions)

> Плагин **Spigot/Sponge** для интеграции экономики, прав, групп, склада-покупок, банов и сбора статистики между сайтом и серверами.

#### Поддерживаемые ядра
- Spigot/Bukkit 1.7.10 - 1.18.1, также их форки по типу PaperSpigot.
- Гибритные ядра: Thermos, Mohist, Magma и т. д.
- Sponge Vanilla и Sponge Forge.

#### Опциональные зависимости
* **LuckPerms** - включит модуль выдачи донат-прав/групп (Spigot/Sponge)
* **BanManager** - включит модуль двухнаправленной интеграции банов с сайтом (Spigot/Sponge)
* **Essentials/EssentialsX/CMI/Nucleus** - проверка на AFK модулем PlayTime (Spigot/Sponge)
* **Vault** - включит модуль внутриигровой экономики (Spigot)

## Установка и настройка
1. [Создайте API-ключ](https://unicorecms.ru/docs/admin/api-keys#создание-api-ключа) с правом `kernel.unicore.connect`.
2. Поместите Jar-файл в папку плагинов.
3. Произведите настройку файла конфигурации UnicoreConnect.

```yaml
server: id сервера
api:
  url: Адрес UnicoreCMS-сервера
  key: API-ключ
```

## Сборка
UnicoreConnect использует Gradle для обработки зависимостей и сборки.

#### Зависимости
* Java 8 JDK или более поздней версии
* Git

#### Компиляция
```sh
git clone https://github.com/MCXTeam/UnicoreConnect.git
cd UnicoreConnect/
./gradlew build
```

Собранный Jar-файл будет лежать в папке build/libs

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

## Локализация

Для редактирования исходных сообщений, откройте файл плагина, как архив. Сообщения хранятся в файле **acf-unicoreconnect_ru.properties**

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
