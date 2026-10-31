# PokeEdit UI (포켓몬 편집 화면)

/pokeedit 명령어를 일일이 치지 않고 화면에서 파티 포켓몬을 편집하는 모드입니다.

- 버전: 1.3.0
- Minecraft 1.21.1 / Fabric
- 필요한 모드: Cobblemon, Fabric API (Poke Trainer가 있으면 트레이너 포켓몬도 편집)
- 설치: 서버와 클라이언트 모두에 넣어야 화면(UI)을 쓸 수 있습니다. 모드가 없는 사람도 서버에 접속은 할 수 있습니다.

## 다운로드

**[⬇ pokeedit-ui-1.3.0.jar 다운로드](https://github.com/minerapai/pokeedit-ui/releases/download/v1.3.0/pokeedit-ui-1.3.0.jar)**

받은 jar 파일을 마인크래프트(또는 서버) 폴더의 `mods` 폴더에 넣으면 됩니다. 다른 버전은 [Releases](https://github.com/minerapai/pokeedit-ui/releases) 에 있습니다.

## 기능

- 레벨, 이로치, 성격, 특성, 개체값, 노력치, 기술 편집
- 포켓몬별 종족값 바꾸기
- 내 트레이너에게 맡긴 포켓몬 편집

## 사용법

| 명령어 | 설명 |
|---|---|
| `U` 키 | 내 파티 편집 화면 열기 |
| `/pokeeditui` | 내 파티 편집 화면 열기 |
| `/pokeeditui <플레이어>` | 다른 플레이어의 파티 편집 |
| `/pokeeditui trainer [플레이어]` | 트레이너에게 맡긴 포켓몬 편집 (트레이너가 배틀 중이면 안 됨) |

모든 기능은 서버 관리자(OP)만 쓸 수 있습니다. 싱글플레이에서는 치트 허용이 켜져 있어야 합니다.

## 참고

친구에게 공유할 때: 이 모드는 서버에도 있어야 동작합니다. 클라이언트에만 넣으면 화면이 열리지 않습니다.

## 빌드

```bash
./gradlew build
```

`build/libs/` 에 만들어진 jar 파일을 서버의 `mods` 폴더에 넣습니다.

## 라이선스

MIT License. 출처(minerapai)만 밝히면 누구나 자유롭게 쓰고 고치고 배포할 수 있습니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 보세요.
