# -*- coding: utf-8 -*-
"""
Markdown 转 HTML 转换脚本
为掘金上传准备 HTML 文件
"""

import os
import re
from pathlib import Path

TUTORIAL_DIR = r"D:\Work\ai\agents\agentscope-java\tutorial"
OUTPUT_DIR = r"D:\Work\ai\agents\agentscope-java\tutorial\docs\html"

def convert_markdown_to_html(md_file, output_dir):
    """将 Markdown 转换为 HTML"""
    with open(md_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # 提取标题
    title_match = re.search(r'^#\s+(.+)$', content, re.MULTILINE)
    title = title_match.group(1) if title_match else "AgentScope Java 教程"

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
                # 生成 HTML 表格
                header_row = table_rows[0]
                headers = [h.strip() for h in header_row.split('|') if h.strip()]
                html = '<table border="1" cellpadding="8" cellspacing="0"><thead><tr>'
                for h in headers:
                    html += f'<th>{h}</th>'
                html += '</tr></thead><tbody>'

                for row in table_rows[2:]:  # 跳过分隔线
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

    if table_rows:
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

    content = '\n'.join(new_lines)

    # 处理列表
    content = re.sub(r'^\s*-\s+(.+)', r'<li>\1</li>', content, flags=re.MULTILINE)
    content = re.sub(r'(<li>.*?</li>\n?)+', lambda m: '<ul>' + m.group(0) + '</ul>', content)

    # 处理链接和图片
    content = re.sub(r'!\[([^\]]*)\]\(([^\)]+)\)', r'<img src="\2" alt="\1"/>', content)
    content = re.sub(r'\[([^\]]+)\]\(([^\)]+)\)', r'<a href="\2">\1</a>', content)

    # 处理代码行
    content = re.sub(r'`([^`]+)`', r'<code>\1</code>', content)

    # 处理段落
    paragraphs = content.split('\n\n')
    html_paragraphs = []
    for p in paragraphs:
        if p.strip():
            if not p.startswith('<') and not p.startswith('#'):
                html_paragraphs.append(f'<p>{p.strip()}</p>')
            else:
                html_paragraphs.append(p)

    content = '\n'.join(html_paragraphs)

    # 转义 HTML
    content = escape_html(content)

    # 生成完整 HTML
    html = f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title}</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            max-width: 900px;
            margin: 0 auto;
            padding: 20px;
            line-height: 1.6;
            color: #333;
        }}
        h1 {{ color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }}
        h2 {{ color: #34495e; margin-top: 30px; }}
        h3 {{ color: #7f8c8d; }}
        code {{
            background: #f4f4f4;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: "Consolas", "Monaco", monospace;
        }}
        pre {{
            background: #2d2d2d;
            color: #f8f8f2;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
        }}
        pre code {{
            background: none;
            padding: 0;
            color: inherit;
        }}
        table {{
            border-collapse: collapse;
            margin: 20px 0;
            width: 100%;
        }}
        th, td {{
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }}
        th {{ background: #3498db; color: white; }}
        tr:nth-child(even) {{ background: #f9f9f9; }}
        a {{ color: #3498db; text-decoration: none; }}
        a:hover {{ text-decoration: underline; }}
        ul {{ padding-left: 20px; }}
        li {{ margin: 5px 0; }}
        blockquote {{
            border-left: 4px solid #3498db;
            padding-left: 15px;
            color: #666;
            margin: 15px 0;
        }}
    </style>
</head>
<body>
{content}
</body>
</html>'''

    return html

def escape_html(text):
    """转义 HTML 特殊字符"""
    return (text.replace('&', '&amp;')
             .replace('<', '&lt;')
             .replace('>', '&gt;')
             .replace('"', '&quot;'))

def main():
    """主函数"""
    docs_dir = Path(TUTORIAL_DIR) / 'docs'
    output_dir = Path(OUTPUT_DIR)
    output_dir.mkdir(parents=True, exist_ok=True)

    # 创建索引页
    md_files = list(docs_dir.glob('*.md'))
    index_content = ['# AgentScope Java 教程\n',
                     '完整的中文教程，包含 19 个章节的理论讲解和实战案例。\n',
                     '## 目录\n']

    for md_file in sorted(md_files):
        if md_file.name != 'README.md':
            title_match = re.search(r'^#\s+(.+)$', md_file.read_text(encoding='utf-8'), re.MULTILINE)
            title = title_match.group(1) if title_match else md_file.stem.replace('-', ' ').title()
            html_name = md_file.stem + '.html'
            index_content.append(f'- [{title}](html/{html_name})\n')

    # 创建索引 HTML
    index_html = convert_markdown_to_html(
        Path(TUTORIAL_DIR) / 'docs' / 'README.md',
        output_dir
    )
    with open(output_dir / 'index.html', 'w', encoding='utf-8') as f:
        f.write(index_html)

    # 转换每个 md 文件
    for md_file in md_files:
        if md_file.name != 'README.md':
            print(f"转换: {md_file.name}")
            html = convert_markdown_to_html(md_file, output_dir)
            output_file = output_dir / (md_file.stem + '.html')
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(html)

    print(f"\n完成！生成了 {len(md_files)} 个 HTML 文件")
    print(f"输出目录: {OUTPUT_DIR}")

if __name__ == "__main__":
    main()