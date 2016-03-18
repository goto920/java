import jp.kmgoto.musicplayer.PanPercFilter;

public class OpenFilterEditor {

/*
   public OpenFilterEditor(){
   }
*/

  public void setAdvancedFilter(PanPercFilter filter){
    System.out.println("setAdvancedFilter() called");
  }

  public void openFilterEditor(){
     new FilterEditor(this);
  }

  public static void main(String[] args){
     OpenFilterEditor app = new OpenFilterEditor();
     app.openFilterEditor();
  }

}
