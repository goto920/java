プログラム例 (作成中)

1. 再生速度可変プレーヤ (作業時間数日)

 TimeStretchPlayerGUI.java 
 TimeStretchPlayer.java -- main()を詳しくかけばコマンド実行可能

コンパイルと実行(簡単)
   rm *.class (念のため) -- 同様にjp/kmgoto/music/*.classも
    javazoom/, org/, pl/ 内のclassファイルは消さない
   javac TimeStretchPlayerGUI.java (他も自動的にコンパイルできる)
   java  TimeStretchPlayerGUI

実行可能jarファイル作成 
   jar cvef TimeStretchPlayerGUI TimeStretchPlayerGUI.jar \
	TimeStretch*.class javazoom jp

jarファイルの実行
   念のため、TimeStretchPlayerGUI.jarだけを別のディレクトリにコピーして
   java -jar TimeStretchPlayerGUI.jar

++++++++++++
2. DrumSuppressorApp (1日)

 若干音切れすることがあるが、リアルタイムでPan, harmonic/percussive
 分離可能なことを確認。プログラムの工夫でもう少し速くできるかも。

  DrumSuppressorApp.java 
   public class DrumSuppressorApp -- GUI
   ファイル内のクラス (publicでない)

   class DrumSuppressPlayer -- Playerスレッド、ファイル保存機能を追加    
   class DrumFilter          -- FFTChunk対象フィルタ

   参考 public, 書かない, protected, privateの比較
          http://ameblo.jp/smeokano/entry-11100092447.html

   コンパイル、実行方法等は同じ

+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
3. FilteredPlayerApp (2日)

   FilterGUI (ボタンでフィルタを設定する別アプリケーション)
   AdvancedFilter (jp/kmgoto/music/Advancedfilter.java)

   FilteredPlayerApp.java
    FilteredPlayerApp -- GUI
     FilterGUIはFilteredPlayerのコンストラクタ内で生成すると別ウィンドウで動く
     スライダはTime, Volumeだけ
    FilteredPlayer -- プレーヤ(Runnable)
     (DrumSuppressPlayerとほとんど同じ -- フィルタが違うだけ)
 
一応、再生しながら変更できる。
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
このさき

プリセット 
  A) カラオケ(センターボーカル消去) 
  B) ドラム音を小さく
-- FilterGUIにsaveFilter, loadFilterを追加
   AdvancedFilter.javaのクラス宣言にimplements Serializableを追加
   すれば、あとはjavaの標準機能でOK。save, loadはクラス外部で操作
   するのが普通らしい

出力先トラックtypeミックスレベル
ある程度の時間範囲かmean square peak のSpectrogramとボタンを重ねて視覚的に
自動フィルタ

++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

