package com.app.model;

import com.framework.tool.*;

public class Article extends Core {
	
	public static String tablename() {
		String clazz = new Object() {
			public String get() {
				String clazz = this.getClass().getName();
				return clazz.substring(0, clazz.lastIndexOf('$'));
			}
		}.get();
		return com.framework.tool.Common.uncamelize(clazz.substring(clazz.lastIndexOf(".")+1));
	}
	
	//获取文章详情
	public DataMap detail(int id) {
		return Db.name("article").where("status=1 AND id="+id).find();
	}

	//分类列表
	public DataList categories() {
		return categories(0);
	}
	public DataList categories(int parent_id) {
		DataList rs = Db.name("article_category").where("status=1 AND parent_id="+parent_id).order("sort ASC, id ASC").field("*, NULL as categories").select();
		if (rs != null) {
			for (DataMap g : rs) {
				if (g.getInt("parent_id") > 0) g.put("categories", this.categories(g.getInt("parent_id")));
			}
			rs = Common.add_domain_deep(rs, "pic");
		}
		return rs;
	}

	//关联图片
	public DataList pics(int article_id, int limit) {
		return Db.name("article_pic").where("article_id="+article_id).order("id ASC").pagesize(limit).field("pic").select();
	}

	//关联商品
	public DataList goods(int article_id) {
		return Db.name("article_goods ag").leftJoin("goods g", "goods_id=g.id").where("article_id="+article_id).order("ag.id ASC").select("g.id, g.name, g.model, g.pic, g.price");
	}

	//是否点赞
	public int liked(int member_id, int article_id) {
		if (member_id == 0) return 0;
		return Db.name("article_like").where("article_id='"+article_id+"' AND member_id='"+member_id+"'").count();
	}
}
