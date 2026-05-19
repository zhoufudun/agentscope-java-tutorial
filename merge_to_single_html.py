# -*- coding: utf-8 -*-
"""
合并所有 Markdown 到一个 HTML 文件
用于掘金部署
"""

import os
import re
from pathlib import Path

TUTORIAL_DIR = r"D:\Work\ai\agents\agentscope-java\tutorial\docs"

def escape_html(text):
    """转义 HTML 特殊字符"""
    return (text.replace('&', '&amp;')
             .replace('<', '&lt;')
             .replace('>', '&gt;')
             .replace('"', '&quot;'))

def convert_markdown(content):
    """将 Markdown 转换为 HTML"""
    # 处理代码块
    def code_block(match):
        lang = match.group(1) or ''
        code = match.group(2)
        return f'<pre><code class="language-{lang}">{escape_html(code)}</code></pre>'

    content = re.sub(r'```(\w+)?\n(.*?)```', code_block, content, flags=re.DOTALL)

    # 处理标题
    content = re.sub(r'^###\s+(.+)$', r'<h3>\1</h3>', content, flags=re.MULTILINE)
    content = re.sub(r'^##\s+(.+)$', r'<h2>\1</h2>', content, flags=re.MULTILINE)
    content = re.sub(r'^#\s+(.+)$', r'<h1>\1</h1>', content, flags=re.MULTILINE)

    # 处理表格
    lines = content.split('\n')
    in_table = False
    table_rows = []
    new_lines = []

    for line in lines:
        if re.match(r'\|.+\|', line):
            if not in_table:
                in_table = True
                table_rows = []
            table_rows.append(line)
        else:
            if in_table and table_rows:
                headers = [h.strip() for h in table_rows[0].split('|') if h.strip()]
                html = '<table border="1" cellpadding="8" cellspacing="0"><thead><tr>'
                for h in headers:
                    html += f'<th>{h}</th>'
                html += '</tr></thead><tbody>'
                for row in table_rows[2:]:
                    if not re.match(r'\|[\s\-:|]+\|', row):
                        cells = [c.strip() for c in row.split('|') if c.strip()]
                        html += '<tr>'
                        for cell in cells:
                            html += f'<td>{cell}</td>'
                        html += '</tr>'
                html += '</tbody></table>'
                new_lines.append(html)
                table_rows = []
                in_table = False
            new_lines.append(line)

    content = '\n'.join(new_lines)

    # 处理列表
    content = re.sub(r'^\s*-\s+(.+)', r'<li>\1</li>', content, flags=re.MULTILINE)
    content = re.sub(r'(<li>.*?</li>\n?)+', lambda m: '<ul>' + m.group(0) + '</ul>', content)

    # 处理链接和图片
    content = re.sub(r'!\[([^\]]*)\]\(([^\)]+)\)', r'<img src="\2" alt="\1"/>', content)
    content = re.sub(r'\[([^\]]+)\]\(([^\)]+)\)', r'<a href="\2">\1</a>', content)

    # 处理代码行
    content = re.sub(r'`([^`]+)`', r'<code>\1</code>', content)

    return content

def main():
    """主函数"""
    docs_dir = Path(TUTORIAL_DIR)
    md_files = sorted(docs_dir.glob('*.md'))

    all_content = []
    toc_items = []

    for md_file in md_files:
        if md_file.name == 'README.md':
            continue

        print(f"处理: {md_file.name}")
        content = md_file.read_text(encoding='utf-8')

        # 提取标题
        title_match = re.search(r'^#\s+(.+)$', content, re.MULTILINE)
        title = title_match.group(1) if title_match else md_file.stem

        # 提取章节号
        chapter_match = re.search(r'第([一二三四五六七八九十\d]+)章', title)
        chapter_num = chapter_match.group(1) if chapter_match else md_file.stem[-2:]

        # 添加锚点和内容
        anchor = md_file.stem
        toc_items.append(f'<li><a href="#{anchor}">{chapter_num} {title.replace("第" + chapter_match.group(1) + "章 ", "") if chapter_match else title}</a></li>')

        # 转换内容
        html_content = convert_markdown(content)
        all_content.append(f'<section id="{anchor}"><div class="chapter-content">{html_content}</div></section>')

    # 生成完整 HTML
    html = f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AgentScope Java 完整教程</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        html {{ scroll-behavior: smooth; }}
        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            background: #f5f5f5;
        }}
        .header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 60px 20px;
            text-align: center;
        }}
        .header h1 {{ font-size: 2.5em; margin-bottom: 10px; }}
        .header p {{ font-size: 1.2em; opacity: 0.9; }}

        /* 导航栏 */
        .nav {{
            background: white;
            padding: 15px 20px;
            position: sticky;
            top: 0;
            z-index: 100;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            overflow-x: auto;
            white-space: nowrap;
        }}
        .nav ul {{
            display: flex;
            gap: 15px;
            list-style: none;
            padding: 0;
            margin: 0;
            justify-content: center;
            flex-wrap: wrap;
        }}
        .nav a {{
            color: #667eea;
            text-decoration: none;
            padding: 8px 12px;
            border-radius: 20px;
            font-size: 0.9em;
            transition: all 0.3s;
        }}
        .nav a:hover {{
            background: #667eea;
            color: white;
        }}

        /* 内容区域 */
        .content {{
            max-width: 1000px;
            margin: 0 auto;
            padding: 20px;
        }}
        section {{
            background: white;
            margin: 30px 0;
            padding: 40px;
            border-radius: 15px;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }}
        .chapter-content h1 {{
            color: #2c3e50;
            border-bottom: 3px solid #667eea;
            padding-bottom: 15px;
            margin-bottom: 30px;
        }}
        .chapter-content h2 {{
            color: #34495e;
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #eee;
        }}
        .chapter-content h3 {{
            color: #7f8c8d;
            margin-top: 20px;
        }}
        .chapter-content p {{
            margin: 15px 0;
            text-align: justify;
        }}
        .chapter-content code {{
            background: #f4f4f4;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: "Consolas", "Monaco", monospace;
            color: #e74c3c;
        }}
        .chapter-content pre {{
            background: #2d2d2d;
            color: #f8f8f2;
            padding: 20px;
            border-radius: 8px;
            overflow-x: auto;
            margin: 20px 0;
        }}
        .chapter-content pre code {{
            background: none;
            padding: 0;
            color: inherit;
        }}
        .chapter-content table {{
            border-collapse: collapse;
            margin: 20px 0;
            width: 100%;
        }}
        .chapter-content th, .chapter-content td {{
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }}
        .chapter-content th {{
            background: #667eea;
            color: white;
        }}
        .chapter-content tr:nth-child(even) {{
            background: #f9f9f9;
        }}
        .chapter-content ul {{
            padding-left: 25px;
            margin: 15px 0;
        }}
        .chapter-content li {{
            margin: 8px 0;
        }}
        .chapter-content blockquote {{
            border-left: 4px solid #667eea;
            padding-left: 15px;
            color: #666;
            margin: 15px 0;
            background: #f9f9f9;
            padding: 15px;
            border-radius: 0 8px 8px 0;
        }}

        /* 返回顶部按钮 */
        .back-to-top {{
            position: fixed;
            bottom: 30px;
            right: 30px;
            background: #667eea;
            color: white;
            width: 50px;
            height: 50px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            text-decoration: none;
            font-size: 24px;
            box-shadow: 0 5px 20px rgba(0,0,0,0.3);
            transition: all 0.3s;
        }}
        .back-to-top:hover {{
            background: #764ba2;
            transform: translateY(-5px);
        }}

        /* 页脚 */
        .footer {{
            text-align: center;
            padding: 40px 20px;
            background: #2c3e50;
            color: white;
            margin-top: 50px;
        }}
        .footer a {{
            color: #667eea;
            text-decoration: none;
        }}
    </style>
</head>
<body>
    <div class="header">
        <h1>AgentScope Java 完整教程</h1>
        <p>基于 Java 21 + Spring Boot 3 的 AI Agent 开发框架，从入门到精通</p>
    </div>

    <nav class="nav">
        <ul>
            <li><a href="#chapter01-overview">概述</a></li>
            <li><a href="#chapter02-quickstart">快速开始</a></li>
            <li><a href="#chapter03-message-session">消息会话</a></li>
            <li><a href="#chapter04-react-agent">ReAct Agent</a></li>
            <li><a href="#chapter05-tools">工具系统</a></li>
            <li><a href="#chapter06-model-integration">模型集成</a></li>
            <li><a href="#chapter07-memory">记忆系统</a></li>
            <li><a href="#chapter08-multi-agent">多代理</a></li>
            <li><a href="#chapter09-a2a-protocol">A2A协议</a></li>
            <li><a href="#chapter10-rag">RAG</a></li>
            <li><a href="#chapter11-skills">技能系统</a></li>
            <li><a href="#chapter12-plan">计划任务</a></li>
            <li><a href="#chapter13-hook">Hook钩子</a></li>
            <li><a href="#chapter14-hitl">人类在环</a></li>
            <li><a href="#chapter15-harness">Harness</a></li>
            <li><a href="#chapter16-springboot">Spring集成</a></li>
            <li><a href="#chapter17-deployment">部署</a></li>
            <li><a href="#chapter18-other-frameworks">其他框架</a></li>
            <li><a href="#chapter19-comprehensive-cases">综合案例</a></li>
        </ul>
    </nav>

    <div class="content">
        {"".join(all_content)}
    </div>

    <a href="#" class="back-to-top">↑</a>

    <div class="footer">
        <p>AgentScope Java Tutorial | 技术栈: Java 21 + Spring Boot 3.2.5 + Lombok + Commons Lang3</p>
        <p>GitHub: <a href="https://github.com/agentscope-ai/agentscope-java-tutorial">agentscope-java-tutorial</a></p>
    </div>
</body>
</html>'''

    output_file = docs_dir / 'complete-tutorial.html'
    output_file.write_text(html, encoding='utf-8')
    print(f"\n完成！生成文件: {output_file}")
    print(f"文件大小: {len(html) / 1024:.1f} KB")

if __name__ == "__main__":
    main()