# План: meet-captions-android

Открытые баги, планы и идеи. Дата: 2026-11-05. Закрытый пункт удаляется или получает ✅ со ссылкой на коммит.

## Баги

Источник: [logic-map-fixes](docs/plans/2026-11-05-logic-map-fixes.md) (#N), [system-design Risks](docs/system-design.md#risks).

- [x] ✅ 2026-11-05 HIGH: debug-дамп каждые 5 с без лимита (36-144 MB/ч, допущение), плюс содержимое Gmail; единственный способ забить диск. `DebugRecorder.kt:66`; logic-map #10, #11. Сделано: один перезаписываемый `dump_*.txt` на рекордер; Gmail-дамп (#11) остаётся; не закоммичено
- [ ] MEDIUM: короткий снимок (<3 слов) обрезает блок, хвост теряется. `CaptionMerger.kt:19-42`; logic-map #1
- [ ] MEDIUM: ASR правит одно из первых 3 слов, снимок дублируется новым блоком. `CaptionMerger.kt:39`; logic-map #2 (вопрос владельцу)
- [ ] MEDIUM: строка с `:` в первых 40 символах считается спикером и перебивает имя из узла. `Transcript.kt:11,14-19`; logic-map #3
- [ ] MEDIUM: при смене спикера старому ходу отдаётся только предпоследний блок, при двух сменах между снимками текст теряется. `Transcript.kt:48-50`; logic-map #4
- [x] ✅ 2026-11-05 MEDIUM: ошибка записи глотается, уведомление всё равно говорит "запись идёт" (полный диск = потерянный звонок). `CaptionService.kt:68-81,98-101`; logic-map #5. Сделано: `writeAtomic` возвращает false, `save` не обновляет уведомление (частично: явного сигнала об ошибке нет, результат `mkdirs` не проверяется); не закоммичено
- [x] ✅ 2026-11-05 MEDIUM: запись целого файла не атомарна, kill посреди записи может обрезать единственную копию. `CaptionService.kt:68`; проверить в `Transcript.kt`; system-design Risks. Сделано: temp-файл + rename (`FileIo.kt`, `FileIoTest`); не закоммичено
- [ ] MEDIUM: сохранение и обход дерева в main-thread handler, стоимость на событие не измерена, риск jank/ANR. `CaptionService.kt:47`; system-design Risks
- [ ] MEDIUM: всё состояние в памяти, смерть процесса делит звонок на два файла и теряет до 3 с. `CaptionService.kt:42,96`; system-design Risks
- [ ] LOW: logic-map #6-#8, #11 (коллектор на все корни Meet, пустой ход в файле, угаданный id `caption`, дамп Gmail; #9 поздняя запись после `onDestroy` исправлена 2026-11-05, не закоммичено): см. [logic-map-fixes](docs/plans/2026-11-05-logic-map-fixes.md)

## Планы и фичи

- [ ] Порядок исправлений: #1-#4 вместе (общие тесты `CaptionMerger`/`Transcript`), #5 отдельно, #6-#8 уборка, #9-#11 вместе с debug-режимом. [logic-map-fixes](docs/plans/2026-11-05-logic-map-fixes.md#order-of-work)
- [ ] Закрыть "Done when": у каждой находки ✅ или решение "оставить" с причиной; в Known gaps карты все открытые HIGH/MEDIUM. [logic-map-fixes](docs/plans/2026-11-05-logic-map-fixes.md#done-when)
- [ ] Следующая ступень: запись файла в фоновый поток через temp-файл + rename, когда в logcat виден ANR/jank или найден обрезанный транскрипт. [system-design](docs/system-design.md) раздел 8

## Идеи

- [ ] Append-only журнал, если потеря при kill посреди звонка случится больше одного раза. [system-design](docs/system-design.md) раздел 8
- [ ] Дамп долгого звонка для нечёткого сопоставления якорного слова (#2) и подтверждения id `caption` (#8). [logic-map-fixes](docs/plans/2026-11-05-logic-map-fixes.md)
- [ ] LOW: нет наблюдаемости кроме logcat, смена UI Meet ломает запись молча (нет блоков, файла, ошибки). [system-design Risks](docs/system-design.md#risks)
- [ ] LOW: ~36 MB/ч износа флеш от перезаписи файла раз в 3 с. `CaptionService.kt:94`; [system-design Risks](docs/system-design.md#risks)
- [ ] Если уведомления запрещены, сигнала о работе записи нет. [system-design](docs/system-design.md) таблица отказов

## Подробные планы

- [docs/plans/2026-11-05-logic-map-fixes.md](docs/plans/2026-11-05-logic-map-fixes.md): находки карты логики, порядок работ
- [docs/system-design.md](docs/system-design.md): разбор по system-design (2026-11-05); сводка по всем репо: `~/dev/docs/system-design-report-2026-11-05.md`
- [docs/logic-map.md](docs/logic-map.md): как субтитр Meet становится файлом
- [docs/architecture.md](docs/architecture.md), [docs/decisions.md](docs/decisions.md)
- [docs/skills-roadmap-2026-11-05.md](docs/skills-roadmap-2026-11-05.md): навыки с рынка вакансий, 2 пункта: JUnit на CaptionMerger/Transcript; unit+detekt в CI и запись файла в корутинах
