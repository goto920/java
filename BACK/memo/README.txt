実行可能jarファイル作成

ファイル配置

MusicPlayer/
    AudioFile.java -- メインプログラム
       import jp.kmgoto.musicplayer.FFTEffector;
       import jp.kmgoto.musicplayer.PanCanceler;
       import jp.kmgoto.musicplayer.PercussionSplitter;
       import jp.kmgoto.musicplayer.Utils;
    Manifest.txt -- 作る(ファイル名は任意)
      Manifest-Version: 1.0 
      Main-Class: AudioFile -- mainがあるクラス名 

      Class-Path: JTransforms-3.1-with-dependencies.jar
      -- だめみたい(jarが古い?)
 
    jp/kmgoto/musicplayer/ -- package jp.kmgoto.musicplayerのソースコード
          FFTEffector.java
          PanCanceler.java
          PercussionSplitter.java
          Utils.java
    JTransforms-3.1....jar -- FFTライブラリ(classのjarファイル)
      jar xvf で展開すると色々できる
+++++++++++++++++++++++++++++++++++
コンパイル

javac -classpath . AudioFile.java 

  classpathに.を入れれば、current dirとその下はコンパイルできるようだ

jarでまとめる (mとfの順はどちらでもよい) -- jarはtarのまね

jar cvmf Manifest.txt musicplayer.jar .  
         *********m*  **f*************

ソースコードも入ってしまうがままいいか。
 
結果確認

実行

 他のディレクトリでmusicplayer.jarだけ使う

 java -jar musicplayer.jar コマンド引数
 (Windowsの場合、クリックするだけで起動するかも)

 java -jar musicplayer.jar SadWoman.wav PAN
  (クラス名は入れない)

++++++++++++++++++++
メッセージの日本語、英語切り替え

propatiesを利用する。

http://tercel-tech.hatenablog.com/entry/2014/10/04/172829

http://www.bohyoh.com/Java/FAQ/FAQ00036.html
+++++++++++++
import java.util.Locale;
import java.util.Locale;

public class PrintLocale {

    public static void main(String[] args) {
        Locale  loc = Locale.getDefault();      // 現在のロケール

        System.out.println("Locale           = "  + loc.toString());

        System.out.println("Country          = "  + loc.getCountry());
        System.out.println("Display Country  = "  + loc.getDisplayCountry());

        System.out.println("Language         = " + loc.getLanguage());
        System.out.println("Display Language = " + loc.getDisplayLanguage());
    }
}
++++++++++++++++++
処理速度 

jump.wav       240秒

リアルタイム再生したい

FFTTEST         27
PAN             43
PERC           143
 ------------------- PAN + PERC = 186
SPERC(Filter)  268 filter.getVerdict()が遅いかも  
 filterから配列をもらってローカルで処理すると少し速くなったかな
なんどかやってみる-- CF-W7で曲の時間より短ければ速いPCなら
 なんとか再生においつくかな。
  227, 263, 239

標準出力にだして、playコマンドで確認

うーんPANでもbuffer underrunだ playのほうでバッファする?
  処理 | play -t raw -r 44100 -s -c 2 -b 16 -
         --input-buffer 20000 (default 8192)
         -q quiet 
         &> /dev/null で表示はなんとか消せる。
  soxの不具合か?

aplayを使うと
  こっちのほうが調子よい
  aplay -t raw -f cd - かな?
       -B, --buffer-time=#
              Buffer  duration is # microseconds If no buffer time and no buf‐
              fer size is given then the maximal allowed buffer time  but  not
              more than 500ms is set.
       -q
       -- あまり意味無し
      &> /dev/null

       Delay for automatic PCM start is  #  microseconds  (relative  to
              buffer size if <= 0)

BufferedOutputStreamのバッファサイズを指定する? default 512
参考
http://www.limy.org/program/java/java_basic1.html

次の課題: 聞きながらのフィルタ変更

Surface Pro 3
 SPERC  62 sec
 PERC   33 sec
 PAN     7.3
 FFTTEST 5.85 sec

Jarファイルの中のファイル(filter samples)の読み込み
http://d.hatena.ne.jp/kirifue/20090610/1245215935
同じjarファイルの中だったら、
BufferedReader reader = getJarReader("data/data.txt");
String line = reader.readLine();
http://d.hatena.ne.jp/Kazuhira/20120311/1331461906
dir も大丈夫かな?
https://teratail.com/questions/26323
ここまでいちどまとめるかなあ。
++++++++++++++++++++++++++++++
