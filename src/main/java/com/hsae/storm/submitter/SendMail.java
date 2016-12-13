package com.hsae.storm.submitter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

public class SendMail {
	private String from; // 发送人
	private String smtpServer; // smtp地址.
	private String userName;
	private String password;
	private String subject;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getSmtpServer() {
		return smtpServer;
	}

	public void setSmtpServer(String smtpServer) {
		this.smtpServer = smtpServer;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public SendMail() {
		this.from = "15518977794@163.com";
		this.smtpServer = "smtp.163.com";
		this.userName = "15518977794@163.com";
		this.password = "xxxxx";
		this.subject="Storm Topology Monitor Alert";
	}


	// 将字符串转换为中文,否则标题会发生乱码现象,QQ邮箱为UTF-8.用GBK.GB2312都会乱码.
	public String translateChinese(String strText) {
		try {
			// MimeUtility.encodeText(String text, String charset, String
			// encoding) throws java.io.UnsupportedEncodingException
			// text 头值 . charset 字符集。如果此参数为 null，则使用平台的默认字符集。
			// encoding 要使用的编码。当前支持的值为 "B" 和 "Q"。如果此参数为 null，则在大部分字符使用 ASCII
			// 字符集编码时使用 "Q" 编码；其他情况使用 "B" 编码。
			strText = MimeUtility.encodeText(new String(strText.getBytes(), "gbk"), "gbk", "B");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return strText;
	}

	public boolean send(String[] toPerson,String content) {
		// 创建邮件Session所需的Properties对象.API建议使用set而不是put(putall).
		Properties props = new Properties();
		props.setProperty("mail.smtp.host", smtpServer);
		props.setProperty("mail.smtp.auth", "true");
		props.put("mail.smtp.ssl.enable", "false");
		// props.put("mail.debug", "true");
		// 创建Session对象,代表JavaMail中的一次邮件会话.
		// Authenticator==>Java mail的身份验证,如QQ邮箱是需要验证的.所以需要用户名,密码.
		// PasswordAuthentication==>系统的密码验证.内部类获取,或者干脆写个静态类也可以.
		Session session = Session.getDefaultInstance(props, new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password);
			}
		});

		try {
			// 构造MimeMessage并设置相关属性值,MimeMessage就是实际的电子邮件对象.
			MimeMessage msg = new MimeMessage(session);
			// 设置发件人
			msg.setFrom(new InternetAddress(from));
			int size = toPerson.length;
			InternetAddress[] addresses =new InternetAddress[size];
			// 设置收件人,为数组,可输入多个地址.
			for(int i=0;i<size;i++){
				String address = toPerson[i];
				addresses[i]=new InternetAddress(address);
			}
			// Message.RecipientType==>TO(主要接收人),CC(抄送),BCC(密件抄送)
			msg.setRecipients(Message.RecipientType.TO, addresses);
			msg.setSentDate(new Date());
			// 设置邮件主题,如果不是UTF-8就要转换下.MimeUtility.encodeText(subject,"gb2312","B"));
			// subject=translateChinese(subject);
			msg.setSubject(MimeUtility.encodeText(subject, "utf8", "B"));
			// =====================正文部分===========
			// 构造Multipart容器
			Multipart mp = new MimeMultipart();
			// =====================正文文字部分===========
			// 向Multipart添加正文
			MimeBodyPart mbpContent = new MimeBodyPart();

			mbpContent.setContent(content, "text/plain;charset=gb2312");
			// 将BodyPart添加到MultiPart容器中
			mp.addBodyPart(mbpContent);

			// 向MimeMessage添加Multipart
			msg.setContent(mp);
			msg.setSentDate(new Date());
			// 发送邮件,使用如下方法!
			Transport.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void main(String[] args) {
//		SendMail mail =new SendMail();
//		List<String> toPerson=new ArrayList<String>();
//		toPerson.add("2452064683@qq.com");
//		toPerson.add("17600803026@163.com");
//		boolean send=false;
//		while (!send) {
//			send = mail.send(toPerson,"请到公司进行处理任务");
//			System.out.println("Successful!");
//		}
	}

}
