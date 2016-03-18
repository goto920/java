java Swing GUIを使うバージョン

計算処理はjp/kmgoto/musicplayer/*.java
import jp.kmgoto.musicplayer.クラス名;

このディレクトリにはSwingを使うGUIのコード、
Javaのサウンド機能を使う部分だけ用意する。

参考にするプログラム (かなり多い)

wav file only (Java Sound API)
http://www.codejava.net/coding/java-audio-player-sample-application-in-swing
 Last Updated on 09 August 2015

http://www.jsresources.org/examples/

Appletとして作れる可能性もある (java.awt.*)

https://docs.oracle.com/javase/jp/1.5.0/guide/sound/programmer_guide/contents.html

http://www.javaworld.com/article/2076227/java-se/add-mp3-capabilities-to-java-sound-with-spi.html

Decoder
http://jaadec.sourceforge.net/usage.php
++++++++++++++++++++++++++++
3/9

Swing入門

http://wisdom.sakura.ne.jp/system/java/swing/

http://sunjava.seesaa.net/category/3714338-1.html
AWT, Swing
SWT (platform dependent)

http://www.02.246.ne.jp/~torutk/javahow2/swing.html

部品の名前
JButton -- ボタン
JLabel  -- 文字列表示(1行)、複数行にはテクニックが必要
           <HTML></HTML>でかけば<BR>が有効らしい
JSlider -- スライダー
JComboBox -- 項目選択

イベント処理

http://www.atmarkit.co.jp/ait/articles/0608/19/news014_2.html

public class SwingAppMain implements ActionListener {
  
 コンストラクタ   
  addButton.addActionListener(this);

public void actionPerformed(ActionEvent event){
 if (event.getSource() == addButton) // private 変数?
   イベントの種類、テキスト値等で分類?

public static void main(String[] args) {
                new SwingAppMain();
        }


画面切り替え
 ページ遷移の際に次のページを追加して、今のページを削除する
 複数用意しておいて切り替える

http://oshiete.goo.ne.jp/qa/4598165.html
CardLayout
 container.invalidate();
container.validate();

JFileChooser (Color?)
  http://waman.hatenablog.com/entry/20110209/1297199326
他

javax.sound.sampled.AudioSystem
たぶん、wavなどは簡単に入力できるだろうが、ミキサーとか
余計なものがある。

url指定、ストリーム
https://docs.oracle.com/javase/jp/1.5.0/guide/sound/programmer_guide/chapter1.html#111814

https://docs.oracle.com/javase/jp/1.5.0/guide/sound/programmer_guide/chapter7.html
+++++++++++++++++++++
Components

https://da2i.univ-lille1.fr/doc/tutorial-java/ui/features/components.html
JProgressBar
JSeparator

http://docs.oracle.com/javase/tutorial/uiswing/layout/gridbag.html
GridBagLayout
NetBeans IDE
jnlp?
http://www.maroon.dti.ne.jp/koten-kairo/works/Java3D/applet4.html
Java Web Start対応Javaアプリケーションを作るには、作成したSwing
アプリケーションに、Java Web Startの仕様に従って記述されたXML形式
のファイル（拡張子*.jnlpファイル）を追加してウェブサーバに
アップロードする。
http://jehupc.exblog.jp/11939745/

netbeansをダウンロード? Ubuntuパッケージで180MBもあるのでやめた

http://www.javadrive.jp/tutorial/jprogressbar/

Audioはswing内で解決するが、ビデオには
  Java Media Framework (オプション?) が必要
http://d.hatena.ne.jp/Guernsey/20090713/1247493375
 import javax.media.*

JMF 
http://www.oracle.com/technetwork/java/javase/tech/index-jsp-140239.html
(SPARCとか書いてあるけど、さすがにWindowsでSparcはないだろう。
mp4がつかえない。2003年だ。ふるいな)
extends the Java 2 Platform, Standard Edition (J2SE)
http://www.oracle.com/technetwork/java/javase/download-142937.html

 If you are installing the JMF Performance Pack for Linux
    Change directories to the install location.
    Run the command % /bin/sh ./jmf-2_1_1e-linux-i586.bin

https://ja.wikipedia.org/wiki/Java_Media_Framework
に他のツールがリストされている

http://fmj-sf.net/ -- 2007 (ましなほう?)

JavaFX (Oracle) -- 余計なものが、、 
 SwingにかわるUI (swing内から使えるらしいが)
 JavaSE 8についている FXMLなるもので大体UIが作れる?
 Java7にも?

JCodec でいいんじゃないか?
http://jcodec.org/
(mp4はあるのに、mp3がない) -- 主にビデオ用か?

http://www.javazoom.net/javalayer/javalayer.html
JLayer, JLayerME 2007 -- 古いけど使えそう

http://jaadec.sourceforge.net/
JAAD mp4 decoder -- mp3もあればなあ

Tutirial JavaFX
https://blog.idrsolutions.com/2014/11/write-media-player-javafx-using-netbeans-ide/

http://www.xuggle.com/public/documentation/java/api/
Java SE 7 Update 2（2011年12月）以降では、JavaFX SDKがJDK 7に含まれています。
そうか、openjdkだとだめなんだ。

インストール (パッケージで)

sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java7-installer 
どっちにしようかな
sudo apt-get install oracle-java8-installer 

sudo update-alternatives --config java
選択を番号で変更
sudo update-alternatives --config javac

他にも/usr/bin/java* があるが、多分全部変更された
+++++++++++++
3/11

javafs.scene.media

Media コンストラクタには、URI文字列 (file:パス)
作っただけでは情報取得できない
MediaPlayerにつないでそのハンドラで情報をgetしたときに
わかるらしい。
結局、GUIの中で再生中しか使えない。
decoderもはいってないように思える。

mp4(AAC) は jaadecでOK?

http://www.javazoom.net/mp3spi/mp3spi.html
http://www.javazoom.net/mp3spi/documents.html (使えそう)
  JLayerを使っている

これとJAADECの両方を使えば解決?
簡単のためにファイル変換ユーティリティとしておくか。

とりあえずMIME typeだけ
https://docs.oracle.com/javase/7/docs/api/java/nio/file/spi/FileTypeDetector.html

Files.probeContentType(path)
++++++++++++++
compile

goto@goto-CF-W7BWYAXP:~/SVN/percussionSplitter/branch/Java/MusicPlayer/Swing$ javac -classpath ../../srclib/ MusicPlayerApp.java 

execution

goto@goto-CF-W7BWYAXP:~/SVN/percussionSplitter/branch/Java/MusicPlayer/Swing$ java -classpath ../../srclib/:. MusicPlayerApp

jar packaging

play raw file
 play -r 44100 -e signed -b 16 -c 2 jump.wav-tmp.raw

+++++++++++++++++++++++
3/13 MP3SPI
http://www.javazoom.net/mp3spi/mp3spi.html
jar file for JLayer, Tritonus and MP3SPI

Tritonusはどこに?
とりあえずJLayerだけでいいかな。

使い方は簡単そう
Converter converter = new Converter(); converter.convert(sourceName, destinationName);

MP3は完了

MP4は
http://jaadec.sourceforge.net/usage.php
stand aloneプログラム例あり
net.sourceforge.jaad.Main [-mp4] <infile> <outfile>	decodes an AAC file to a WAV file
一応完了、時々不具合
+++++++++++++++++
3/14

24bit wavサポート -- Effectorは面倒なので、16bitのまま
48kHzサポート -- Effector内部のSAMPLING_RATEを外から設定すれば大丈夫か?

入力ファイル input.ext
  -- 16bit wavで中間出力 (input.ext.wav)

Utilsにその機能(boolean anyToWaveFile())を作る?
(falseなら処理できなかった意味)
  ファイル拡張子だけで判定
    mp4ビデオ, mp3, m4a, aac が変換可能(24bitは16bit wav, rateはそのまま)
    wavのときは24bitは16bitに変換 input.ext-16bit.wav
    変換なしのときもおなじ?
(だいたいできている)

Effectorの処理対象 (input.wav or input.ext-16bit.wav)
 Effector (inputをrawではなくwavファイルに変更)
   AudioInputStreamでsampling rate 他を検査
 出力 input.ext-conv.wav (従来とおり)

大改造
  Advanced setting 周波数とpan範囲をボタンで選択
 -- 別ウィンドウでpop upして、選択した結果を表示
    よかったらそれをmainプログラムに戻して利用
  要変更 PanCanceler -- かんたん
  PercussionSplitter -- 現在panを使っていないので、
  PanCancelerのPan計算結果を利用するように変更する必要あり

Java AudioInputStream -- 24bitだめ、48kHzだめ
  自分で作るしかないかなあ、、
-- そうでもないみたい。48kHzはOK、見つかった。
   24bitはだめ、formatすらunsupported
   面倒なので、変換系は別のアプリケーションをつくろう
   sampling rateはAudioOutputStreamに渡せば変換してくれるみたいだ。
   だから、AudioInputStreamを自分バージョンにすればよい?
   24bit --> 16bitは簡単、各チャネルのLSBを無視すればよいだけ。
   wave headerは自分で分解する必要あり。

   file type 
    http://www.tutorialspoint.com/tika/tika_document_type_detection.htm

++++++++++++
変換Playテスト

-------- wav original and 24bit, 48k, 48k-24bit
1) OK -rw-rw-r-- 1 goto goto 42520656  3月 15 10:45 /tmp/jump.wav
2) OK -rw-rw-r-- 1 goto goto 63780944  3月 15 10:45 /tmp/jump24.wav
 conv -rw-rw-r-- 1 goto goto 42520620  3月 15 11:04 jump24.wav-16bit.wav
3) -rw-rw-r-- 1 goto goto 46280944  3月 15 10:46 /tmp/jump48k.wav
 変換しない(正常)、再生できない(PlayerThread?)
4) -rw-rw-r-- 1 goto goto 69421430  3月 15 10:46 /tmp/jump48k24.wav
  -rw-rw-r-- 1 goto goto 46280944  3月 15 11:07 jump48k24.wav-16bit.wav
 変換OK、再生できない(PlayerThread?)
-------------------------------------------------
パッケージのコンフリクト
http://stackoverflow.com/questions/17823542/conflicting-jar-methods
http://stackoverflow.com/questions/10666023/jaad-stopping-other-providers-from-working

jar ライブラリをunloadできる?
http://abondar-howto.blogspot.jp/2010/06/howto-unload-jar-files-loaded-by.html
やっぱり無理そう、すべての参照を消せば消えるはずだが。。
ファイル変換は別プログラムにしよう(GUIいるかなあ)

ConverterThreadの仕様変更 -- wavファイルを処理

+++++++++++++
