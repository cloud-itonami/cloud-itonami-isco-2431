# physai-isco-2431 — 広告・マーケティング専門家（ISCO 2431）の印刷とサイン施工を支えるロボットの physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isco-2431`、ISCO 2431 広告・マーケティング専門家）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: 印刷・サインロボットが、校正刷りの物理的な印刷とサイン（看板）施工の支援を行う。
その物理的な仕事を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:sign-panel-to-mount-height` | manipulator | 印刷したサインパネルをカートから壁の取付け高さまで持ち上げる（2 リンクアーム） | 肩関節ピークトルク | 350 N·m（estimate） |
| `:panel-cart-to-site` | transport | 立てたサインパネルのカートを印刷室から店内の施工場所へ運ぶ（AMR） | 最小転倒余裕 | 0.3 以上（estimate） |
| `:hanger-rod-tension` | material | 天井サインを吊る M6 鋼製吊りボルト（強度区分 4.6）を引張る（kudaki J2 トラス） | 最終ひずみ | 0.002（estimate。降伏 240 MPa・有効断面積 20.1 mm² は ISO 898-1） |

測定の入口: `kbb -M:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:test`（`test/advertising_marketing/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する）。
この repo 自身の `.kotoba` test は kbb では走らない（fleet の JVM gate が走らせる）。この bot の test 数は physics の test だけを数える。

## 測って分かったこと・限界（成長の第一候補）

1. **アーム**: 肩トルクは 2 kg で 82.04 N·m、8 kg で 131.5 N·m、16 kg で 197.7 N·m。取付け高さ 1.0 m まで上げる動作で関節仕事は 111.2〜248.5 J。
   限界 350 N·m に達する積荷は **34.38 kg**。
2. **搬送**: 転倒余裕は重心高さだけで決まり（ブレーキ減速度 1.0 m/s² が支配）、0.6 m で 0.755、1.2 m で 0.511、1.8 m で 0.266。
   限界 0.3 を割る重心高さは **1.716 m**。背の高いパネルを立てて運ぶときの制約はここ。
3. **吊りボルト**: 4.5 kN までは最終ひずみ 0.0011 以下の弾性域、5 kN で 0.0055、6 kN で 0.0571 と塑性へ跳ぶ。
   限界 0.002 を越える荷重は **4,900 N**（公称降伏荷重 240 MPa × 20.1 mm² = 4,824 N の直上）。
4. **estimate のままの値**: 肩トルク上限 350 N·m（20 kg 級産業用ロボットの仕様書）、転倒余裕 0.3（AMR メーカーの安定性基準）、
   吊りボルトの判定ひずみ 0.002（吊り金物の設計許容応力、例えば建築基準の許容引張応力で置き換える）、アーム・AMR の寸法・質量。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isco-2431 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:test → kbb -M:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isco-2431 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。
