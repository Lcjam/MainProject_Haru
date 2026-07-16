---
name: haru-verify
description: Haru 백엔드 3개 + 프론트를 서버 기동 없이 빌드/컴파일 검증한다. 빌드 확인, 컴파일 체크, CI식 회귀 검증 요청 시 사용.
---

# Haru 빌드 검증 (서버 기동 없이)

리팩토링 중 회귀 확인용. **장기 실행 서버(`bootRun`) 금지**, 컴파일/빌드만.

## 전제: JDK 17 toolchain
3개 Spring 서비스 모두 `build.gradle`에 Java **17** 고정. 머신엔 JDK 21만 있을 수 있으므로 `~/.gradle/gradle.properties`에 17 경로 등록 필요:
```
org.gradle.java.installations.paths=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```
없으면 `brew install openjdk@17` 후 위 파일 작성. 확인: `cd GateWay && ./gradlew -q javaToolchains | grep "JDK 17"`.

## 백엔드 컴파일 체크 (테스트 제외)
```bash
cd /Users/jamie/Desktop/Project/MainProject_Haru
for s in GateWay CoreService AssistService; do
  echo "=== $s ==="; ( cd "$s" && ./gradlew clean compileJava -x test --console=plain 2>&1 | tail -5 )
done
```

## 백엔드 테스트 포함 빌드
```bash
( cd CoreService && ./gradlew build --console=plain 2>&1 | tail -20 )
```

## 프론트 빌드 + 테스트
```bash
cd /Users/jamie/Desktop/Project/MainProject_Haru/vite-react-teamsketch
npm run build        # tsc -b && vite build (타입체크 포함)
npm test 2>/dev/null || echo "test 스크립트 없음(추가 예정)"
```

## 리팩토링 지표 수집 (before/after용)
```bash
cd /Users/jamie/Desktop/Project/MainProject_Haru
echo "backend System.out.println:"; grep -rl "System.out.println" {GateWay,CoreService,AssistService}/src/main/java 2>/dev/null | wc -l
echo "frontend console.log:"; grep -rn "console.log" vite-react-teamsketch/src 2>/dev/null | wc -l
echo "frontend any:"; grep -rn ": any\|<any>\|as any" vite-react-teamsketch/src 2>/dev/null | wc -l
echo "largest component:"; find vite-react-teamsketch/src -name "*.tsx" -exec wc -l {} + | sort -rn | head -3
```
