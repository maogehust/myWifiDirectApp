package com.example.android.wifidirect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.filebrowser.utils.ImageLoader;
import com.example.filebrowser.utils.ImageLoaderConfig;
import com.example.filebrowser.utils.ImageWorker;
import com.fl.adapter.MyImageAdapter;
import com.fl.adapter.MySubImageAdapter;
import com.fl.utils.MimeUtil;
import com.wifidirect.entity.ImageGroup;
import com.wifidirect.ui.MyStickyLayout;
import com.wifidirect.ui.MyStickyLayout.OnGiveUpTouchEventListener;

public class FileBrowser extends Fragment implements OnGiveUpTouchEventListener{
	
	   File currentParent;
	   File[] currentFiles;
	   File[] orderFiles;
	   
	   List<ImageGroup> imageGroups;
	   List<String> imageFiles;
	   ListView listView;
	   GridView gridView;
	   TextView textView;
	   Button button;
	   Button sendButton;
	   long currentTime = 0;
	   MyStickyLayout stickyLayout;
	   
	   public int BitmapWidth;
	   
	   //分别对应图片，文件，视频的按钮
	   private Button imageButton;
	   private Button fileButton;
//	   private Button videoButton;
	   
	   private int imageBackTimes;
	   private static final int IMAGE_VIEW = 1;
	   private static final int FILE_VIEW = 2;
	   private static final int VIDEO_VIEW = 3;
	   private int viewType = FILE_VIEW;
	   
	   private Map<String, List<String>> mGroupMap = new HashMap<String,List<String>>();
	   
	   //ImageWorker imageWorker;   需要通过config初始化
	   ImageLoader imageWorker = ImageLoader.getInstance();
	   
	   
	   View mContentView = null;
	   

	   MySubImageAdapter mSubImageAdapter = null;
	   ImageAdapter imageAdapter;
	   MyImageAdapter mImageAdapter = null;
	   
	   /*
	    * 保存多选信息
	    */
	   //是否多选
	   private boolean isMultiSelect = false;
	   //记录每个位置是否被选择
	   private HashMap<Integer, Boolean> itemStatus = new HashMap<Integer,Boolean>();
	  
	   //将已被选择的文件加入List
	   private ArrayList<String> selectedItems = new ArrayList<String>();
	   
	   //当前文件夹下的子文件
	   List<Map<String, Object>> listItems;
	   //选择后的对勾
	   private static  Bitmap selectedBitmap;
	  
	   //未选择时的对勾
	   private static Bitmap unSelectedBitmap;
	   
	   //文件和文件夹图标
	   private static Bitmap fileBitmap;
	   private static Bitmap dirBitmap;
	   
	   
	   /*
	    * 保存屏幕位置信息
	    */
	   int index;
	   int top;
	   List<Integer[]> posInfo = new ArrayList<Integer[]>();
	   
		@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		
		//imageWorker = new ImageWorker(this.getActivity());
		this.getActivity();
//		imageWorker = ImageWorker.imageWorkerFactory(this.getActivity());
//		
//		imageWorker.setLoadingImage(R.drawable.empty_photo);

		//Application app = this.getActivity().getApplication();
		
		//初始化layout布局
		stickyLayout = (MyStickyLayout) mContentView.findViewById(R.id.sticky_layout);
		imageButton = (Button) stickyLayout.findViewById(R.id.image_button);
	    fileButton = (Button)stickyLayout.findViewById(R.id.file_button);
//	    videoButton = (Button)stickyLayout.findViewById(R.id.video_button);
		
		//listView = (ListView) mContentView.findViewById(R.id.list);
		textView = (TextView) mContentView.findViewById(R.id.text);
		gridView = (GridView) mContentView.findViewById(R.id.list);
		button = (Button)mContentView.findViewById(R.id.select);
		sendButton = (Button) mContentView.findViewById(R.id.send);
		
		changeButtonState(false);//创建时默认为未连接，不显示button
		
		//为多选对勾 初始化
		selectedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.btn_check_on_selected);
		unSelectedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.btn_check_off);
		
		//初始化bitmap大小
		BitmapWidth = this.getActivity().getResources().getDimensionPixelSize(R.dimen.image_size);
		
				
		//为文件夹和文件图标初始化,这里没有用到，直接使用了资源文件
		//fileBitmap =  BitmapFactory.decodeResource(getResources(), R.drawable.wenjian);
		//dirBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.high_wjj1);
		
		//开启新线程，获取图片组信息
		getImages();
		
		//根节点
		File root;
		root = new File("/");
		
		
		if(root.exists()){
			currentParent = root;
			currentFiles = orderFiles(root.listFiles());
			
			inflateListView(currentFiles);
		}
		
		imageButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//初始化图片文件夹信息
				if(imageGroups == null){
					imageGroups = subGroupOfImage(mGroupMap);
				}
				
				if(mImageAdapter == null){
				 mImageAdapter = new MyImageAdapter(getActivity(), imageGroups);
	
				}
				gridView.setAdapter(mImageAdapter);
				gridView.setNumColumns(1);
				viewType = IMAGE_VIEW;
				imageBackTimes = 0;
			    	
			}
		});
		
		fileButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				if(imageAdapter == null){
					return;
				}
				gridView.setAdapter(imageAdapter);
				gridView.setNumColumns(4);
				viewType = FILE_VIEW;
				
			}
		});
		
		
		
		sendButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// 回调接口发送文件
				if(selectedItems!= null && selectedItems.size() != 0){
					 SendFileCallbackListener sendFileListener = (SendFileCallbackListener) getActivity();
					 sendFileListener.sendFile(selectedItems);
				}
			  
			    
			}
			
 		});
		
		
	  /*多选按钮*/
      button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// 多选切换按钮
				
				//如果由多选模式切换回，清空选择的信息
				if(isMultiSelect){
				//清空已选择的文件路径
				selectedItems = new ArrayList<String>();
				//清空图标信息
				clearSelectInfo();
				}
				isMultiSelect = !isMultiSelect;
				//通知各个adapter更新状态
				imageAdapter.changeStatus(-1);
				mSubImageAdapter.notifyIsSelect(isMultiSelect);
				
				
			}
		});
      
      
      
      
      

		//为listView 监听屏幕位置信息
		gridView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO 弄清楚这里的记录屏幕位置信息的原理？
				if(scrollState == OnScrollListener.SCROLL_STATE_IDLE){
					 index = gridView.getFirstVisiblePosition();
					View v = gridView.getChildAt(0);
					
					top =(v==null)?0:(v.getTop()-gridView.getPaddingTop());
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				
				
			}
		});
      
      
		
      
            //设置点击条目的监听器
    		gridView.setOnItemClickListener(new OnItemClickListener(){

    			@Override
    			public void onItemClick(AdapterView<?> parent, View view,
    					int position, long id) {
    				
    				/**
    				 * 判断是否进入多选模式
    				 */
    				if(isMultiSelect){
    					if(viewType == FILE_VIEW){
    						imageAdapter.changeStatus(position);
        					selectedItems.add(currentFiles[position].getAbsolutePath());
    					}else if(viewType == IMAGE_VIEW){
    						mSubImageAdapter.changeStatus(position);
    						selectedItems.add(imageFiles.get(position));
    					}
    					
    				}else{
    					if(viewType == FILE_VIEW){
    						if(currentFiles[position].isDirectory()){
        						currentParent = currentFiles[position];
        						currentFiles = orderFiles(currentParent.listFiles());
        						/**
        						 * 进入文件夹时保存屏幕位置信息
        						 */
        						posInfo.add(new Integer[]{index,top});
        						
        						//绘制子文件目录
        						inflateListView(currentFiles);
        						}else{
        							String mimeType = MimeUtil.getMimeType(currentFiles[position]);
        							Intent intent = new Intent(Intent.ACTION_VIEW);
        							intent.setDataAndType(Uri.parse("file://"+currentFiles[position].getAbsolutePath()), mimeType);
        							intent.addCategory(Intent.CATEGORY_DEFAULT);
        							startActivity(intent);
        							
        							Log.i("fl", currentFiles[position].getName());
        							
        						}
    					}else if(viewType == IMAGE_VIEW){
    						if(imageBackTimes == 0){//从group组进入child组
    							Iterator it = mGroupMap.entrySet().iterator();
        						if(mGroupMap.size() < position){
        							return;
        						}
        						for(int i = 0; i!=position; i++){
        							it.next();
        						}
        						Entry<String, List<String>>  entry = (Map.Entry<String,List<String>>)it.next();
        						imageFiles = entry.getValue();
        						
        						if(mSubImageAdapter == null){
        							itemStatus = new HashMap<Integer, Boolean>(2*imageFiles.size());
        							mSubImageAdapter = new MySubImageAdapter(FileBrowser.this.getActivity(), imageFiles, itemStatus);
        						}else{
        							mSubImageAdapter.setDataSet(imageFiles);
        						}
        						gridView.setAdapter(mSubImageAdapter);
        						gridView.setNumColumns(3);
        						imageBackTimes++;
        						
    						}
    					
    					}
    					
    					
    						
    				}
    				
    				
    				
    			}
    			
    		});
    		
    		
    		stickyLayout.setOnGiveUpTouchEventListener(this);
      


		/*
		 * fragment中监听返回按钿
		 */
		
		//为什么要加这两行
		getView().setFocusableInTouchMode(true);
	    getView().requestFocus();
	    
	    getView().setOnKeyListener(new View.OnKeyListener() {
	        @Override
	        public boolean onKey(View v, int keyCode, KeyEvent event) {

	            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){

	                // handle back button
	            	Date date = new Date();
	    			
	            	if(viewType == FILE_VIEW){
	            		if(currentParent.getAbsolutePath().equals("/")){
		    				if(currentTime==0||(date.getTime()-currentTime>1000)){
		    					//TODO:获取activity的方式正确吗＿
		    					Toast.makeText(getActivity(), "再按一次返回键退出",Toast.LENGTH_SHORT).show();
		    				    currentTime = date.getTime();
		    				}else{
		    					getActivity().finish();
		    				}
		    					
		    			}
		    			else if(currentParent.getParentFile()!=null){
		    				currentParent = currentParent.getParentFile();
		    			    currentFiles = orderFiles(currentParent.listFiles());
		    			    inflateListView(currentFiles);
		    			}
	            		 return true;
	            	}
	            	
	            	else if(viewType == IMAGE_VIEW || viewType == VIDEO_VIEW){
	            		if(imageBackTimes == 0){
	            			if(currentTime==0||(date.getTime()-currentTime>1000)){
		    					//TODO:获取activity的方式正确吗＿
		    					Toast.makeText(getActivity(), "再按一次返回键退出",Toast.LENGTH_SHORT).show();
		    				    currentTime = date.getTime();
		    				}else{
		    					getActivity().finish();
		    				}
	            		}else{
	            			if(imageGroups == null){
	        					imageGroups = subGroupOfImage(mGroupMap);
	        				}
	        				
	        				if(mImageAdapter == null){
	        				 mImageAdapter = new MyImageAdapter(getActivity(), imageGroups);
	        	
	        				}
	        				gridView.setAdapter(mImageAdapter);
	        				gridView.setNumColumns(1);
//	        				viewType = IMAGE_VIEW;
	        				imageBackTimes--;
	            		}
	            		
	            		return true;
	            	}
	    			

	            }

	            return false;
	        }

			
	    });
	}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			
			
			mContentView = inflater.inflate(R.layout.filebrowser,container,false);
			
			return mContentView;
			
		}

		/**
		 * 根据连接状态改变button的visible
		 * @param isconnect
		 */
		public void changeButtonState(boolean isconnect){
//			LayoutInflater inflater = LayoutInflater.from(this.getActivity());
//			LinearLayout ly = (LinearLayout) inflater.inflate(R.id.filebrowser_linear1, null);
			LinearLayout ly = (LinearLayout)mContentView.findViewById(R.id.filebrowser_linear1);
			if(!isconnect){
				
				ly.setVisibility(View.GONE);
//				sendButton.setVisibility(View.INVISIBLE);
//				button.setVisibility(View.INVISIBLE);
			}else{
				ly.setVisibility(View.VISIBLE);
				
//				sendButton.setVisibility(View.VISIBLE);
//				button.setVisibility(View.VISIBLE);
			}
		}
		
		
		
		private File[] orderFiles(File[] currentFiles){
			List<File> files = new ArrayList<File>();
			List<File> dirFiles = new ArrayList<File>();
			//判断currentFiles是否为空
			if(currentFiles==null){
				return currentFiles;
			}else{
				File[] orderFiles = new File[currentFiles.length];
				
				for(File file:currentFiles){
					if(file.isDirectory())
						dirFiles.add(file);
					else
						files.add(file);
				}
				
				
		         TreeSet<File> dirtree = new TreeSet<File>(dirFiles);
		         TreeSet<File> tree = new TreeSet<File>(files);
		         Iterator<File> it = dirtree.iterator();
		         
		         int i=0;
		         while(it.hasNext()){
		        	 orderFiles[i] = it.next();
		        	
		        	 i++;
		         }
		         
		         it = tree.iterator();
		         while(it.hasNext()){
		        	 orderFiles[i] = it.next();
		        	 i++;
		         }
		         
		         return orderFiles;
			}
			
	         
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
			
			
			
		}

		
		
		
		
		
		/**
		 * 填充listView
		 * @param files 将被显示的文件
		 */
		private void inflateListView(File[] files){
			//每次重新初始化子文件List
		    listItems = new ArrayList<Map<String, Object>>();
			
			int i = 0;
			if(files!=null){
			for(File file:files){  //如果是文件夹
				String name = file.getName().toLowerCase();
				HashMap<String, Object> map = new HashMap<String, Object>();
				if(file.isDirectory())
					map.put("icon", R.drawable.high_wjj1);
				//如果是图片
				else if(name.contains(".jpg")||name.contains(".png")){
					
					String imagePath = file.getAbsolutePath();
					
					map.put("icon", imagePath);
					
					
				}
				//如果是文件
				else
					map.put("icon", R.drawable.wenjian);
				map.put("fileName", file.getName());
				listItems.add(map);
				
				itemStatus.put(i++, false);
				
			}
		}
			
			//初始化imageAdapter
			imageAdapter = new ImageAdapter(this.getActivity(),listItems);
			
			gridView.setAdapter(imageAdapter);
			
			
			try {
				textView.setText("当前路径为"+currentParent.getCanonicalPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		public class ImageAdapter extends BaseAdapter{

			private LayoutInflater mInflater;
			private List<Map<String, Object>> data;
			
			public ImageAdapter(Context context, List<Map<String, Object>> list){
				mInflater = LayoutInflater.from(context);
				data = list;
			}
			
			private void changeStatus(int position){
				
				if(position != -1)
				itemStatus.put(position, !itemStatus.get(position));
				//通知适配器进行更新
				notifyDataSetChanged();
			}
			
			
			//TODO:这个有什么用    返回指定数据对应的视图类型
			@Override
			public int getItemViewType(int position) {
				
				String filename = String.valueOf(data.get(position).get("fileName")).toLowerCase();
				return isImage(filename) == true ? 1:0;
			}
	        
			//返回视图类型总数
			@Override
			public int getViewTypeCount() {
				return 2;
			}
	        
			//返回数据总数
			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return data.size();
			}
	        
			
			@Override
			public Object getItem(int position) {
				// TODO Auto-generated method stub
				return data.get(position);
			}

			@Override
			public long getItemId(int position) {
				// TODO Auto-generated method stub
				return position;
			}
	       
			//填充视图并返回
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// TODO Auto-generated method stub
				String name = (String) data.get(position).get("fileName");
				ViewHolder viewHolder;
				Log.i("fl---getView", name);
				System.out.println(data.get(position).get("icon"));
				//如果对应的视图不存在，创建
				if(convertView == null){
					viewHolder = new ViewHolder();
					if(isImage(name.toLowerCase())){
						Log.i("fl", "图片路径");
					//为contentView填充，并绑定控件
					convertView = mInflater.inflate(R.layout.image_list_line, parent, false);
					viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_list_image);
					viewHolder.textView = (TextView) convertView.findViewById(R.id.image_list_text);
					viewHolder.check_imageView = (ImageView) convertView.findViewById(R.id.image_list_check);
						
						convertView.setTag(viewHolder);
						
					}else{
						Log.i("fl", "文件路径");
						convertView = mInflater.inflate(R.layout.gridline, parent, false);
//						viewHolder.imageView = (ImageView) convertView.findViewById(R.id.list_image);
//						viewHolder.textView = (TextView) convertView.findViewById(R.id.list_text);
						viewHolder.imageView = (ImageView) convertView.findViewById(R.id.gridImage);
						viewHolder.textView = (TextView) convertView.findViewById(R.id.gridText);
						viewHolder.check_imageView = (ImageView) convertView.findViewById(R.id.checkImage);
						
						convertView.setTag(viewHolder);
					
					}
				}else{
					Log.i("fl", "复用view");
					viewHolder = (ViewHolder) convertView.getTag();
					
				}
				
				
				Bitmap isCheckImg;
				if(isMultiSelect){
				if(itemStatus.get(position)){
					 isCheckImg = selectedBitmap;
				}else{
				     isCheckImg = unSelectedBitmap;
				}
				
				}else{
					isCheckImg = null;
					//viewHolder.check_imageView.setVisibility(View.GONE);
				}
				//多选状态改变以后，重新设置check_imageView
				viewHolder.check_imageView.setImageBitmap(isCheckImg);
				
				
				
				//加载不同的图片类型
				if(isImage(name.toLowerCase())){
					imageWorker.displayImage((String)data.get(position).get("icon"), viewHolder.imageView, BitmapWidth);
					//imageWorker.loadImage(data.get(position).get("icon"), viewHolder.imageView);
				}else{
					
					
//					Bitmap bit;
//					if((int)data.get(position).get("icon") == R.drawable.wenjian)
//						bit = fileBitmap;
//					else
//						bit = dirBitmap;
					/*
					Drawable[] drawArr = new Drawable[2];
					drawArr[0] = new BitmapDrawable(getResources(), bit);
					drawArr[1] = new BitmapDrawable(getResources(), isCheckImg);
					
					LayerDrawable layerDrawable = new LayerDrawable(drawArr);
					layerDrawable.setLayerInset(0, 0, 0, 0, 0);
					layerDrawable.setLayerInset(1, 0, 0, 90, 90);
					*/
					viewHolder.imageView.setImageResource((Integer)data.get(position).get("icon"));
					
				}
				viewHolder.textView.setText(name);
				return convertView;
			}
			
			private class ViewHolder{
				private TextView textView;
				private ImageView imageView;
				private ImageView check_imageView;
			}
			
		}
		
		
		
		private boolean isImage(String name){
			if(name.contains(".jpg")||name.contains(".png"))
				return true;
			return false;
		}
		
		/**
		 * 解析出图片组的信息
		 */
		private void getImages(){
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
					ContentResolver mContentResolver = getActivity().getContentResolver();
				
						Cursor mCursor = mContentResolver.query(mImageUri, null, 
								MediaStore.Images.Media.MIME_TYPE+"=? or " + MediaStore.Images.Media.MIME_TYPE +"=?", 
								new String[]{"image/jpeg","image/png"}, MediaStore.Images.Media.DATE_MODIFIED);
						if(mCursor == null){
							return;
						}
						while(mCursor.moveToNext()){//将查询到的文件路径加入map
							String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
							String parentName = new File(path).getParentFile().getName();
							if(!mGroupMap.containsKey(parentName)){
								List<String> childList = new ArrayList<String>();
								childList.add(path);
								mGroupMap.put(parentName, childList);
							}else{
								mGroupMap.get(parentName).add(path);
							}
						}
					}
				
				
				
			}).start();
		}
		
		/**
		 * 解析出图片组的信息
		 * @param map
		 * @return
		 */
		private List<ImageGroup> subGroupOfImage(Map<String,List<String>> map){
			List<ImageGroup> list = new ArrayList<ImageGroup>();
			if(map.isEmpty()){
				return null;
			}
			Iterator<Map.Entry<String, List<String>>> iterator = map.entrySet().iterator();
			while(iterator.hasNext()){
				Map.Entry<String, List<String>> entry = iterator.next();
				String key = entry.getKey();
				List<String> value = entry.getValue();
				
				ImageGroup group = new ImageGroup();
				group.setCount(value.size());
				group.setParentName(key);
				group.setTopImagePath(value.get(0));//获取该组第一个元素
				list.add(group);
			}
			return list;
		}
		
		/**
		 * 清除已选择的信息
		 */
		public void clearSelectInfo(){
			for(Entry<Integer,Boolean> set:itemStatus.entrySet()){
				set.setValue(false);
			}
		}
		
		
		
		
		public interface SendFileCallbackListener{
			public void sendFile(ArrayList<String> list);
		}
		
		



		@Override
		public boolean giveUpTouchEvent(MotionEvent event) {
			Log.i("fl----位置", gridView.getFirstVisiblePosition()+"");
			if(gridView.getFirstVisiblePosition() == 0){
				View view = gridView.getChildAt(0);
				if(view != null && view.getTop()>=0){
					return true;
				}
			}
			return false;
		}


	


	}
