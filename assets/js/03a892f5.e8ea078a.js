"use strict";(self.webpackChunkmy_website_ts=self.webpackChunkmy_website_ts||[]).push([[737],{5708:function(e){e.exports=JSON.parse('{"blogPosts":[{"id":"test-blog-post","metadata":{"permalink":"/SQLiter/blog/test-blog-post","editUrl":"https://github.com/touchlab/SQLiter/tree/main/blog/2022-05-20-ok-blog.md","source":"@site/blog/2022-05-20-ok-blog.md","title":"Test Blog Post","description":"This is the summary of a very long blog post,","date":"2022-05-20T00:00:00.000Z","formattedDate":"May 20, 2022","tags":[{"label":"hello","permalink":"/SQLiter/blog/tags/hello"},{"label":"docusaurus","permalink":"/SQLiter/blog/tags/docusaurus"}],"readingTime":0.785,"truncated":true,"authors":[{"name":"Kevin Galligan","title":"Touchlab person","url":"https://www.kgalligan.com","imageURL":"https://github.com/kpgalligan.png","key":"kpgalligan"}],"frontMatter":{"slug":"test-blog-post","title":"Test Blog Post","authors":"kpgalligan","tags":["hello","docusaurus"]}},"content":"This is the summary of a very long blog post,\\n\\nHello2!\\n\\n\x3c!--truncate--\x3e\\n\\n```kotlin\\n@GetMapping(path = [\\"/asPrivateHtml/{docName}\\"])\\n@ResponseBody\\nfun asPrivateHtml(\\n    @PathVariable docName: String,\\n    @RequestHeader(value = \\"fkDocReferrer\\", required = false) fkDocReferrer: String?\\n): ResponseEntity<String> {\\n    val markdown = docContentFromRepo(docName, fkDocReferrer)\\n    val parser = Parser.builder().extensions(listOf(YamlFrontMatterExtension.create())).build()\\n    val document: Node = parser.parse(markdown)\\n    val renderer = HtmlRenderer\\n        .builder()\\n        .build()\\n    val html = renderer.render(document)\\n    return ResponseEntity.ok(html)\\n}\\n```\\n\\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque elementum dignissim ultricies. Fusce rhoncus ipsum tempor eros aliquam consequat. Lorem ipsum dolor sit amet\\n\\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque elementum dignissim ultricies. Fusce rhoncus ipsum tempor eros aliquam consequat. Lorem ipsum dolor sit amet\\n\\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque elementum dignissim ultricies. Fusce rhoncus ipsum tempor eros aliquam consequat. Lorem ipsum dolor sit amet\\n\\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque elementum dignissim ultricies. Fusce rhoncus ipsum tempor eros aliquam consequat. Lorem ipsum dolor sit amet"}]}')}}]);