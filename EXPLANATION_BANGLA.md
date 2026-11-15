# University Management System — স্যারকে বোঝানোর গাইড (বাংলায়)

> এই ফাইলটা পড়ে পড়ে প্র্যাকটিস করো। প্রতিটা অংশে লেখা আছে: **কোন ফাইলে কোড আছে**, **কোড কেমন**, **কেন ব্যবহার করা হয়েছে**।
> স্যার যেটাই জিজ্ঞেস করুক, সেই অংশের ফাইলটা খুলে কোড দেখিয়ে বলবে।

---

## ০. পুরো প্রজেক্ট এক নজরে (১ মিনিটে বলার মতো)

"স্যার, এটা একটা University Management System। এখানে তিন ধরনের user আছে — **Admin, Teacher, Student**। প্রত্যেকের আলাদা table আছে। Login করলে role অনুযায়ী আলাদা dashboard খোলে। Admin সবকিছু manage করতে পারে। Teacher নতুন student register করতে পারে, কিন্তু সেই student **Admin approve না করলে login করতে পারে না**। কেউ শুধু নিজের profile edit/delete করতে পারে। Web page (Thymeleaf) এবং REST API দুটোই আছে। Database MySQL, আর পুরো প্রজেক্ট Docker দিয়ে এক command-এ চলে।"

### Technology গুলো এবং কেন ব্যবহার করেছি

| Technology | কী কাজ করে | কেন ব্যবহার করেছি |
|---|---|---|
| **Java 17** | প্রোগ্রামিং ভাষা | Spring Boot 3-এর জন্য Java 17 লাগে |
| **Spring Boot 3** | Framework | কম configuration-এ web app বানানো যায়, Tomcat server ভেতরেই থাকে |
| **Maven** (`pom.xml`) | Build tool | Library (dependency) download করে আর jar file বানায় |
| **Spring Web** | Controller, REST API | URL-কে Java method-এর সাথে যুক্ত করে |
| **Spring Data JPA + Hibernate** | Database access | SQL না লিখে Java object দিয়ে database-এ save/read করা যায় |
| **MySQL 8** | Database | data স্থায়ীভাবে রাখে |
| **Spring Security** | Login, role, CSRF | নিজে security কোড না লিখে standard way-তে সুরক্ষা পাই |
| **BCrypt** | Password hash | password plain text-এ database-এ রাখা বিপজ্জনক, তাই hash করে রাখি |
| **Validation** (`@NotBlank` ইত্যাদি) | Input যাচাই | ভুল data backend-এ আটকানো যায় |
| **Thymeleaf** | HTML template | Java data সরাসরি HTML page-এ বসানো যায় |
| **Bootstrap 5** (CDN) | CSS design | সুন্দর, responsive UI খুব কম কোডে |
| **Lombok** | Boilerplate কমায় | getter/setter/constructor নিজে লিখতে হয় না |
| **Docker + Compose** | Container | কম্পিউটার যেটাই হোক, একই ভাবে চলে; MySQL আলাদা install করতে হয় না |
| **JUnit + MockMvc + H2** | Test | automatic test; test-এ MySQL লাগে না |

> **JWT, React, microservice** ব্যবহার করিনি — কারণ requirement অনুযায়ী সহজ রাখতে হবে। আমরা **session based login** ব্যবহার করেছি।

---

## ১. Folder structure — কোন ফাইল কোথায়

```
University management/
├── pom.xml                     ← Maven: dependency গুলো
├── Dockerfile                  ← Docker image বানানোর নিয়ম (multi-stage)
├── compose.yaml                ← app + MySQL একসাথে চালানো
├── context.md                  ← requirement
├── src/main/java/com/university/management/
│   ├── UniversityManagementApplication.java   ← main() — এখান থেকে শুরু
│   ├── config/
│   │   ├── SecurityConfig.java   ← 🔐 URL authorization, login, CSRF
│   │   └── DataSeeder.java       ← 🌱 শুরুতে test account বানায়
│   ├── entity/                   ← 🗄️ Database table (Admin, Teacher, Student)
│   ├── repository/               ← 🗄️ Database থেকে data আনা/রাখা
│   ├── dto/                      ← 📦 Request/Response object, validation
│   ├── service/                  ← 🧠 Business rule (email unique, ownership, approval)
│   ├── controller/
│   │   ├── api/                  ← 🌐 REST API (JSON)
│   │   └── web/                  ← 🖥️ Thymeleaf page-এর controller
│   ├── security/                 ← 🔐 CustomUserDetailsService, login handler, 401/403 handler
│   └── exception/                ← ⚠️ Custom exception, global exception handler
├── src/main/resources/
│   ├── application.properties    ← database config
│   ├── templates/                ← Thymeleaf HTML পেজ
│   └── static/css/style.css
└── src/test/java/...             ← Integration test
```

### Request কীভাবে চলে (এই flow টা মুখস্থ রাখো)

```
Browser / Postman
      │  HTTP request
      ▼
Spring Security Filter  ← login আছে কিনা, role ঠিক কিনা (SecurityConfig)
      ▼
Controller              ← request নেয়, @Valid দিয়ে input যাচাই করে
      ▼
Service                 ← business rule (email unique? নিজের record? approve?)
      ▼
Repository              ← database-এ save/find
      ▼
MySQL
```
এটাকে বলে **Layered Architecture**। প্রতিটা layer-এর আলাদা কাজ, তাই কোড সহজে বোঝা যায়।

---

## ২. Database Entity (Table) — Lombok সহ

**ফাইল:** `src/main/java/com/university/management/entity/Student.java` (একইভাবে `Admin.java`, `Teacher.java`)

```java
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;      // BCrypt hash

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false, unique = true)
    private String rollNumber;

    @Column(nullable = false)
    private boolean approved = true;
}
```

**বোঝানোর পয়েন্ট:**
- `@Entity` → এই class-টা database table হবে। `@Table(name="students")` → table-এর নাম।
- `@Id` + `@GeneratedValue(IDENTITY)` → primary key, MySQL নিজে auto increment করে।
- `@Column(nullable=false)` → required (খালি রাখা যাবে না)। `unique = true` → একই email দুইবার না।
- requirement অনুযায়ী **তিনটা আলাদা table** — একটা User table-এ merge করিনি।
- `approved` → Admin বানালে `true`, Teacher বানালে `false` (service-এ ঠিক হয়)।

### Lombok কী? (স্যার নিশ্চিত জিজ্ঞেস করবে)
`@Getter`, `@Setter` লিখলে Lombok **compile-এর সময় নিজে getter/setter বানিয়ে দেয়**। না থাকলে আমাকে `getFullName()`, `setFullName()`, `isApproved()`… সব হাতে লিখতে হতো।
- `@NoArgsConstructor` → খালি constructor (JPA-এর লাগে)।
- `@RequiredArgsConstructor` → `final` field গুলো দিয়ে constructor বানায় — আমরা service/controller-এ এটা দিয়ে **dependency injection** করেছি:

```java
@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;   // Spring নিজে এটা inject করে
    ...
}
```
- `@AllArgsConstructor` → সব field-এর constructor (Response DTO-তে)।
- `@Getter` → শুধু getter। pom.xml-এ Lombok dependency আছে (`<optional>true</optional>`)।

---

## ৩. Repository — Database operation

**ফাইল:** `repository/StudentRepository.java`

```java
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRollNumber(String rollNumber);
    List<Student> findByApproved(boolean approved);   // pending list
    long countByApproved(boolean approved);           // dashboard badge
}
```

**বোঝানোর পয়েন্ট:**
- এটা শুধু **interface** — আমি কোনো SQL লিখিনি। `JpaRepository` থেকে `save()`, `findAll()`, `findById()`, `delete()`, `count()` ফ্রি পাই।
- method-এর **নাম দেখে** Spring Data নিজে SQL বানায়: `findByEmailIgnoreCase` → `SELECT * FROM students WHERE LOWER(email) = LOWER(?)`।
- `existsBy...` → আছে কিনা (true/false)। `countByApproved(false)` → pending কতজন।

---

## ৪. DTO ও Validation

**কেন DTO?** Entity-তে `password` আছে। Entity সরাসরি JSON-এ ফেরত দিলে password hash বাইরে চলে যেত। তাই:
- **Request DTO** = client যা পাঠায় (`StudentRequest`)
- **Response DTO** = client কে যা ফেরত দিই (`StudentResponse`) — **এতে password field নেই**।

**ফাইল:** `dto/StudentRequest.java`
```java
@Getter @Setter @NoArgsConstructor
public class StudentRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @Pattern(regexp = "|.{6,}", message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Semester is required")
    @Min(1) @Max(12)
    private Integer semester;

    @NotBlank(message = "Roll number is required")
    private String rollNumber;
}
```
- `@NotBlank`, `@Email`, `@Min`, `@Max` = **Bean Validation**। Controller-এ `@Valid` দিলে এগুলো check হয়। ভুল হলে → REST-এ **400 Bad Request**, web form-এ লাল error message।
- password-এর regex `|.{6,}` মানে: **খালি (update-এ পুরনো password রাখতে) অথবা কমপক্ষে ৬ অক্ষর**। Create-এর সময় খালি হলে service `400` দেয়।
- `approved` field কোনো request-এ নেই — client নিজে approved=true পাঠাতে পারবে না। Server ঠিক করে।

**ফাইল:** `dto/StudentResponse.java`
```java
public static StudentResponse from(Student student) {
    return new StudentResponse(student.getId(), student.getFullName(), student.getEmail(),
            student.getDepartment(), student.getSemester(), student.getRollNumber(),
            student.isApproved());
}
```
Entity → Response DTO বানায়, password বাদ।

---

## ৫. Service — Business Rule (সবচেয়ে গুরুত্বপূর্ণ অংশ)

### ৫.১ Cross-table email uniqueness
**ফাইল:** `service/EmailChecker.java`
```java
public void ensureAvailable(String email) {
    boolean used = adminRepository.existsByEmailIgnoreCase(email)
            || teacherRepository.existsByEmailIgnoreCase(email)
            || studentRepository.existsByEmailIgnoreCase(email);
    if (used) {
        throw new DuplicateResourceException("email", "Email is already in use: " + email);
    }
}
```
- একটা email তিন table-এর কোথাও দুইবার থাকতে পারবে না। তিনটা service-ই এই class ব্যবহার করে, তাই নিয়মটা এক জায়গায় লেখা।
- `DuplicateResourceException` → **409 Conflict**।
- Update করার সময় email **বদলালে** তবেই check হয় (`TeacherService.update` দেখো):
```java
if (!email.equalsIgnoreCase(teacher.getEmail())) {
    emailChecker.ensureAvailable(email);
}
```
না হলে নিজের email দিয়েই নিজে conflict করত।
- Email সবসময় `trim().toLowerCase()` করে save হয়, তাই `ADMIN@x.com` আর `admin@x.com` একই ধরা হয়।

### ৫.২ Roll number unique
**ফাইল:** `service/StudentService.java`
```java
private void checkRollNumberFree(String rollNumber) {
    if (studentRepository.existsByRollNumber(rollNumber)) {
        throw new DuplicateResourceException("rollNumber", "Roll number is already in use: " + rollNumber);
    }
}
```

### ৫.৩ Ownership check (নিজের record ছাড়া অন্যেরটা বদলানো যাবে না)
**ফাইল:** `service/TeacherService.java` (Student-এও একই)
```java
private void checkOwnership(Teacher teacher, Authentication currentUser) {
    if (AuthUtil.isAdmin(currentUser)) {
        return;                                   // Admin সবকিছু পারে
    }
    if (!teacher.getEmail().equalsIgnoreCase(currentUser.getName())) {
        throw new AccessDeniedException("You can only modify your own profile");
    }
}
```
- `currentUser.getName()` = login করা user-এর email।
- যে teacher-এর email মেলে না → `AccessDeniedException` → **403 Forbidden**।
- `update()` ও `delete()` দুটোতেই প্রথমে `checkOwnership()` ডাকা হয়।
- একজন Teacher কোনো Student edit/delete করতে চাইলেও আটকে যায় (তার email student-এর email-এর সাথে মেলে না)।

### ৫.৪ Approval logic
**ফাইল:** `service/StudentService.java`
```java
// Create: Admin হলে approved = true, Teacher হলে false
student.setApproved(AuthUtil.isAdmin(currentUser));

// Admin approve করলে
public Student approve(Long id) {
    Student student = findById(id);
    student.setApproved(true);
    return studentRepository.save(student);
}

// Admin reject করলে — record delete
public void reject(Long id) {
    studentRepository.delete(findById(id));
}
```

### ৫.৫ Password hash
```java
teacher.setPassword(passwordEncoder.encode(request.getPassword()));
```
`passwordEncoder` = `BCryptPasswordEncoder`। Database-এ থাকে `$2a$10$...` ধরনের hash — আসল password কেউ দেখতে পারবে না।

### `@Transactional` কেন?
`create/update/delete` method-এ `@Transactional` আছে। মানে method-এর সব database কাজ হয় একসাথে — মাঝখানে error হলে সব **rollback** (আধা-আধি data save থাকে না)।

---

## ৬. Authentication (Login) — "তুমি কে?"

### ৬.১ CustomUserDetailsService — তিন table-এ খোঁজা
**ফাইল:** `security/CustomUserDetailsService.java`
```java
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Optional<Admin> admin = adminRepository.findByEmailIgnoreCase(email);
    if (admin.isPresent()) {
        return buildUser(admin.get().getEmail(), admin.get().getPassword(), "ADMIN", true);
    }
    Optional<Teacher> teacher = teacherRepository.findByEmailIgnoreCase(email);
    if (teacher.isPresent()) {
        return buildUser(..., "TEACHER", true);
    }
    Optional<Student> student = studentRepository.findByEmailIgnoreCase(email);
    if (student.isPresent()) {
        // approved না হলে enabled = false → Spring DisabledException ছোড়ে
        return buildUser(..., "STUDENT", student.get().isApproved());
    }
    throw new UsernameNotFoundException("No user found with email " + email);
}

private UserDetails buildUser(String email, String password, String role, boolean enabled) {
    return User.builder()
            .username(email).password(password)
            .roles(role)               // "ADMIN" → "ROLE_ADMIN"
            .disabled(!enabled)
            .build();
}
```
**ব্যাখ্যা:** User login form-এ email/password দিলে Spring Security এই method ডাকে। আমরা Admin → Teacher → Student table পরপর খুঁজি, পেলে role সহ `UserDetails` ফেরত দিই। তারপর Spring নিজে **BCrypt দিয়ে password মেলায়**।

### ৬.২ Pending student login আটকানো
- Student `approved == false` হলে `.disabled(true)` দেওয়া হয়। Spring Security তখন নিজে **`DisabledException`** ছোড়ে, login fail হয়।
- **ফাইল:** `security/LoginFailureHandler.java`
```java
String param = (exception instanceof DisabledException) ? "pending" : "error";
response.sendRedirect(request.getContextPath() + "/login?" + param);
```
- `login.html`-এ `?pending` থাকলে দেখায়: **"Your account is pending admin approval."**

### ৬.৩ Role অনুযায়ী redirect
**ফাইল:** `security/LoginSuccessHandler.java`
```java
String target = "/student/dashboard";
if (AuthUtil.isAdmin(authentication)) {
    target = "/admin/dashboard";
} else if (... "ROLE_TEACHER" ...) {
    target = "/teacher/dashboard";
}
response.sendRedirect(request.getContextPath() + target);
```

### ৬.৪ Session based login — SecurityConfig-এ
**ফাইল:** `config/SecurityConfig.java`
```java
.formLogin(form -> form
    .loginPage("/login")
    .usernameParameter("email")          // form-এর field-এর নাম "email"
    .passwordParameter("password")
    .successHandler(loginSuccessHandler)
    .failureHandler(loginFailureHandler)
    .permitAll())
.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
```
Login সফল হলে server একটা **session** বানায়, browser-এ `JSESSIONID` cookie যায়। পরের request-এ এই cookie দেখে server চিনে নেয় তুমি কে। (JWT লাগেনি।)

### ৬.৫ BCrypt bean
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
BCrypt হলো one-way hash — hash থেকে আসল password বের করা যায় না। প্রতিবার আলাদা "salt" থাকে, তাই একই password-এর hash আলাদা আসে।

---

## ৭. Authorization (Role দিয়ে access control) — "তুমি কী করতে পারবে?"

**Authentication** = তুমি কে (login)। **Authorization** = তোমার কী অনুমতি আছে (role)।

**ফাইল:** `config/SecurityConfig.java`
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/login", "/css/**", "/js/**", "/error").permitAll()

    .requestMatchers("/admin/**", "/api/admins/**").hasRole("ADMIN")

    .requestMatchers(HttpMethod.POST, "/api/teachers", "/api/teachers/**").hasRole("ADMIN")
    .requestMatchers("/teacher/**", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")

    .requestMatchers(HttpMethod.PUT, "/api/students/*/approve").hasRole("ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/students", "/api/students/**").hasAnyRole("ADMIN", "TEACHER")
    .requestMatchers("/student/**").hasAnyRole("ADMIN", "STUDENT")
    .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")

    .anyRequest().authenticated())
```
**লক্ষ করো:** rule গুলো **উপর থেকে নিচে** পড়া হয়, প্রথম যেটা মেলে সেটাই কাজ করে। তাই নির্দিষ্ট rule (যেমন approve) সাধারণ rule-এর **আগে** লিখেছি।

| URL | কে পারবে |
|---|---|
| `/`, `/login`, `/css/**`, `/error` | সবাই |
| `/admin/**`, `/api/admins/**` | শুধু Admin |
| `/teacher/**`, `/api/teachers/**` | Admin, Teacher (নতুন Teacher বানানো শুধু Admin) |
| `/student/**` | Admin, Student |
| `/api/students/**` | Admin, Teacher, Student |
| `PUT /api/students/{id}/approve` | শুধু Admin |
| `POST /api/students` | Admin, Teacher |

> **দুই স্তরের নিরাপত্তা:** URL স্তরে role (SecurityConfig) + data স্তরে ownership (Service)। একজন Teacher `/api/teachers/5` URL-এ ঢুকতে পারে (role ঠিক), কিন্তু ৫ নম্বর যদি অন্য Teacher হয়, service 403 দেয়।

### ৭.১ REST API-এর 401 ও 403 (JSON)
- **`security/RestAuthenticationEntryPoint.java`** — login ছাড়া `/api/...` ডাকলে **401 + JSON**। Browser page হলে `/login`-এ redirect।
- **`security/RestAccessDeniedHandler.java`** — login আছে কিন্তু permission নেই → **403 + JSON**। Browser page হলে 403 error page।
```java
ApiError body = new ApiError(HttpStatus.FORBIDDEN.value(), "Forbidden",
        "You do not have permission to access this resource");
response.setStatus(HttpStatus.FORBIDDEN.value());
response.setContentType(MediaType.APPLICATION_JSON_VALUE);
objectMapper.writeValue(response.getOutputStream(), body);
```
এগুলো `SecurityConfig`-এ বসানো:
```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint(authenticationEntryPoint)
    .accessDeniedHandler(accessDeniedHandler))
```

### ৭.২ CSRF
**CSRF কী?** আমি login করা অবস্থায় কোনো খারাপ ওয়েবসাইট যদি আমার browser দিয়ে চুপিচুপি আমার university site-এ POST request পাঠায় (cookie নিজে নিজে যায়) — এটা CSRF attack।

**সমাধান:** প্রতিটা form-এ একটা গোপন **CSRF token** থাকে; token ছাড়া POST গেলে server **403** দেয়। Spring Security default-এ এটা চালু রাখে। Thymeleaf-এ `th:action` ব্যবহার করলে token আপনা-আপনি hidden field হিসেবে বসে:
```html
<form th:action="@{/login}" method="post"> ... </form>
```
Logout-ও তাই POST form (`fragments/layout.html`)।

API-তে Postman/curl দিয়ে কাজ করার জন্য **HTTP Basic** চালু করেছি (`.httpBasic(...)`) এবং `Authorization` header থাকলে CSRF skip করি:
```java
.csrf(csrf -> csrf.ignoringRequestMatchers(
    request -> request.getHeader("Authorization") != null));
```
কারণ browser কখনো নিজে থেকে `Authorization` header জোড়ে না, তাই সেখানে CSRF attack সম্ভব না। Browser form-এ CSRF **চালু** আছে। (Test: `AuthenticationTest.loginWithoutCsrfTokenIsForbidden`)

---

## ৮. Exception Handling

**ফাইল:** `exception/` ফোল্ডার

| Exception | কখন | HTTP |
|---|---|---|
| `ResourceNotFoundException` | id পাওয়া যায়নি | 404 |
| `DuplicateResourceException` | email/roll number আগে থেকেই আছে | 409 |
| `AccessDeniedException` (Spring-এর) | অন্যের record বদলানোর চেষ্টা | 403 |
| `BadRequestException` | যেমন create-এ password নেই | 400 |
| `MethodArgumentNotValidException` | `@Valid` fail | 400 |

**ফাইল:** `exception/GlobalExceptionHandler.java`
```java
@RestControllerAdvice(basePackages = "com.university.management.controller.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)       // না থাকলে Spring 500 দিত
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }
    ...
}
```
- `@RestControllerAdvice` = সব REST controller-এর exception এক জায়গায় ধরে JSON error বানায়। তাই controller-এ try/catch লিখতে হয় না।
- Error JSON উদাহরণ:
```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "message": "Email is already in use: x@y.com" }
```
- Validation error হলে `fieldErrors` থাকে: `{"fieldErrors": {"email": "Email is not valid"}}`
- **`exception/WebExceptionHandler.java`** — একই কাজ কিন্তু Thymeleaf পেজের জন্য: 403 হলে `error/403.html`, 404 হলে `error/404.html` দেখায়।

---

## ৯. REST API

**ফাইল:** `controller/api/StudentRestController.java`
```java
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentRestController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request,
                                                  Authentication authentication) {
        StudentResponse body = StudentResponse.from(studentService.create(request, authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);        // 201
    }

    @GetMapping
    public List<StudentResponse> getAll() { ... }                            // 200

    @GetMapping("/{id}")
    public StudentResponse getOne(@PathVariable Long id) { ... }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request,
                                  Authentication authentication) { ... }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        studentService.delete(id, authentication);
        return ResponseEntity.noContent().build();                           // 204
    }

    @PutMapping("/{id}/approve")
    public StudentResponse approve(@PathVariable Long id) { ... }
}
```

**শব্দগুলো:**
- `@RestController` = `@Controller` + `@ResponseBody` → return করা object নিজে JSON হয়ে যায়।
- `@RequestMapping("/api/students")` = base URL।
- `@PostMapping/@GetMapping/@PutMapping/@DeleteMapping` = HTTP method।
- `@RequestBody` = JSON কে Java object-এ রূপান্তর। `@PathVariable` = URL-এর `{id}`।
- `@Valid` = validation চালাও।
- `Authentication` = Spring নিজে দেয় — এখন কে login করা আছে (service-এ ownership check এর জন্য পাঠাই)।

### সব endpoint

| Method | URL | কাজ | Status |
|---|---|---|---|
| POST | `/api/admins` | Admin বানাও | 201 |
| GET | `/api/admins` | সব Admin | 200 |
| GET | `/api/admins/{id}` | একজন | 200 / 404 |
| PUT | `/api/admins/{id}` | update | 200 / 404 / 409 |
| DELETE | `/api/admins/{id}` | delete | 204 / 404 |
| একইভাবে | `/api/teachers` ও `/api/students` | | |
| PUT | `/api/students/{id}/approve` | Admin approve | 200 |

**Status code গুলো:** 200 OK, 201 Created, 204 No Content, 400 Bad Request (validation), 401 Unauthorized (login নেই), 403 Forbidden (permission নেই), 404 Not Found, 409 Conflict (duplicate)।

### Terminal থেকে demo (HTTP Basic)
```bash
# সব student দেখা (Admin)
curl -u admin@university.edu:admin123 http://localhost:8080/api/students

# Teacher নতুন student register করছে → approved:false
curl -u sarah.khan@university.edu:teacher123 -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test Student","email":"t@s.edu","password":"secret1","department":"CSE","semester":1,"rollNumber":"CSE-9"}'

# Admin approve করছে
curl -u admin@university.edu:admin123 -X PUT http://localhost:8080/api/students/4/approve

# login ছাড়া → 401
curl -i http://localhost:8080/api/students
```

---

## ১০. Web Interface (Thymeleaf + Bootstrap)

### Controller (web)
**ফাইল:** `controller/web/` — `HomeController`, `AdminWebController`, `AdminTeacherWebController`, `AdminStudentWebController`, `TeacherWebController`, `StudentWebController`

```java
@Controller
@RequestMapping("/admin/students")
public class AdminStudentWebController {

    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("students", studentService.findPending());
        return "admin/pending";       // templates/admin/pending.html
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        studentService.approve(id);
        redirect.addFlashAttribute("success", "Student approved. The student can log in now.");
        return "redirect:/admin/students/pending";
    }
}
```
- `@Controller` → HTML পেজ return করে (view-এর নাম)। `Model` দিয়ে data HTML-এ পাঠাই।
- `redirect:` → কাজ শেষে অন্য page-এ পাঠায় (Post-Redirect-Get — refresh করলে আবার submit হয় না)।
- `RedirectAttributes.addFlashAttribute` → একবারের জন্য সবুজ "success" বার্তা।

### Form + Validation error দেখানো
```java
@PostMapping("/admins")
public String create(@Valid @ModelAttribute("form") AdminRequest form, BindingResult result, ...) {
    if (!result.hasErrors()) {
        try {
            adminService.create(form);
            return "redirect:/admin/admins";
        } catch (DuplicateResourceException e) {
            result.rejectValue(e.getField(), "duplicate", e.getMessage());   // field-এর পাশে লাল error
        }
    }
    return formPage(...);    // error থাকলে form আবার দেখাও
}
```

### Thymeleaf template — গুরুত্বপূর্ণ জিনিস
**ফাইল:** `templates/shared/student-form.html`
```html
<form th:action="@{${formAction}}" th:object="${form}" method="post" novalidate>
    <input type="text" th:field="*{fullName}" class="form-control" th:errorclass="is-invalid">
    <div class="invalid-feedback" th:errors="*{fullName}"></div>
</form>
```
- `th:object` = form কোন Java object-এর সাথে বাঁধা। `th:field` = প্রতিটা input কোন field।
- `th:errors` = validation error message দেখায়; `th:errorclass="is-invalid"` = লাল border (Bootstrap)।
- `th:action` = CSRF token নিজে বসায়।

**ফাইল:** `templates/shared/students.html` (Student list — Admin, Teacher, Student তিনজনেই ব্যবহার করে)
```html
<div th:if="${isAdmin or #strings.equalsIgnoreCase(s.email, currentEmail)}">
    <a th:href="@{${basePath + '/' + s.id + '/edit'}}">Edit</a>
    ... Delete form ...
</div>
```
→ **Edit/Delete বোতাম শুধু Admin অথবা নিজের row-তে** দেখায় (requirement অনুযায়ী)। (তবে শুধু বোতাম লুকানোই যথেষ্ট না — backend-এও check আছে, সেটা service-এ।)

**ফাইল:** `templates/fragments/layout.html` (navbar — সব পেজে একই)
```html
<th:block sec:authorize="hasRole('ADMIN')"> ... Admin-এর menu ... </th:block>
<th:block sec:authorize="hasRole('TEACHER')"> ... Teacher-এর menu ... </th:block>
```
`sec:authorize` (thymeleaf-extras-springsecurity6) → role দেখে menu দেখায়/লুকায়।

### পেজ তালিকা (requirement অনুযায়ী)
| পেজ | ফাইল |
|---|---|
| Login | `templates/login.html` (error / pending / logout message) |
| Admin dashboard (count + Pending badge) | `templates/admin/dashboard.html` |
| Admin/Teacher/Student CRUD | `admin/admins.html`, `shared/teachers.html`, `shared/students.html`, `shared/*-form.html` |
| Pending approval (Approve/Reject) | `templates/admin/pending.html` |
| Teacher dashboard | `templates/teacher/dashboard.html` |
| Teacher: student register (banner সহ) | `shared/student-form.html` (`notice` চালু থাকলে) — `TeacherWebController.registerPage()` |
| Student dashboard | `templates/student/dashboard.html` |

---

## ১১. Seed Data (শুরুতে test account)

**ফাইল:** `config/DataSeeder.java`
```java
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    @Override
    public void run(String... args) {
        if (adminRepository.count() > 0) {
            return;                       // আগেই data থাকলে আর কিছু করবে না
        }
        addAdmin("System Admin", "admin@university.edu", "admin123");
        addTeacher("Dr. Sarah Khan", "sarah.khan@university.edu", "Computer Science", "Professor");
        addStudent("Sakib Hasan", "sakib.hasan@student.edu", "Computer Science", 1, "CSE-2025-032", false);
        ...
    }
}
```
- `CommandLineRunner` → application চালু হওয়ার ঠিক পরে `run()` একবার চলে।
- Password `passwordEncoder.encode(...)` দিয়ে hash হয়ে save হয়।
- `@Profile("!test")` → test চালানোর সময় এটা বন্ধ থাকে।

**Account list:**
| Role | Email | Password |
|---|---|---|
| Admin | `admin@university.edu` | `admin123` |
| Teacher | `sarah.khan@university.edu` | `teacher123` |
| Student (approved) | `ayesha.rahman@student.edu` | `student123` |
| Student (pending — login করতে পারবে না) | `sakib.hasan@student.edu` | `student123` |

---

## ১২. Docker — কোথায় কী আছে এবং কীভাবে কাজ করে

### Docker কেন?
আমার কম্পিউটারে যা চলে, স্যারের কম্পিউটারে না-ও চলতে পারে (Java version, MySQL install…)। Docker পুরো environment (Java + app + MySQL) একটা **container**-এ প্যাকেজ করে — তাই **এক command-এ সবখানে একই ভাবে চলে**: `docker compose up --build`।

### ১২.১ `Dockerfile` (প্রজেক্টের root-এ) — Multi-stage build
```dockerfile
# ---------- Stage 1: Maven দিয়ে jar বানাই ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: ছোট runtime image ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
**লাইন ধরে ব্যাখ্যা:**
- `FROM maven:3.9-eclipse-temurin-17 AS build` → Stage 1: Maven + JDK 17 আছে এমন image; নাম দিলাম `build`।
- `COPY pom.xml .` + `RUN mvn dependency:go-offline` → আগে শুধু pom কপি করে library download করি। এতে Docker এই step **cache** করে — পরে শুধু কোড বদলালে library আবার download হয় না (দ্রুত build)।
- `COPY src ./src` → কোড কপি। `RUN mvn clean package -DskipTests` → jar বানাই (test বাদ, test আলাদা `mvn test`-এ চালাই)।
- `FROM eclipse-temurin:17-jre` → Stage 2: শুধু **JRE** (চালানোর জন্য যথেষ্ট; Maven/JDK/source কিছু নেই)।
- `COPY --from=build /app/target/app.jar app.jar` → Stage 1 থেকে শুধু তৈরি jar আনি। (`pom.xml`-এ `<finalName>app</finalName>` দেওয়া আছে বলে নাম `app.jar`।)
- `EXPOSE 8080` → app 8080 port-এ চলে। `ENTRYPOINT [...]` → container চালু হলে `java -jar app.jar`।

**Multi-stage কেন?** Final image ছোট হয় (Maven, source code, cache থাকে না), নিরাপদ হয়।

### ১২.২ `compose.yaml` — দুইটা container একসাথে
```yaml
name: university-management-system

services:
  db:
    image: mysql:8.0
    container_name: university_db
    environment:
      MYSQL_DATABASE: university_db
      MYSQL_USER: university_user
      MYSQL_PASSWORD: university_pass
      MYSQL_ROOT_PASSWORD: root_pass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: university_app
    depends_on:
      db:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/university_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: university_user
      SPRING_DATASOURCE_PASSWORD: university_pass
    restart: on-failure

volumes:
  mysql_data:
```
**ব্যাখ্যা:**
- **`db` service:** আসল `mysql:8.0` image। `environment` দিয়ে database নাম, user, password সেট হয়।
- **`volumes: mysql_data`** → database-এর ফাইল Docker volume-এ থাকে। Container মুছলেও data থাকে। (data মুছতে চাইলে `docker compose down -v`)
- **`healthcheck`** → প্রতি ১০ সেকেন্ডে `mysqladmin ping` চালায়; ৫ সেকেন্ডের মধ্যে উত্তর না এলে fail; ৫ বার fail হলে "unhealthy"।
- **`depends_on: db: condition: service_healthy`** → app container **শুরুই হবে না** যতক্ষণ MySQL healthy না হয়। এটা না থাকলে Spring Boot MySQL ready হওয়ার আগেই connect করতে গিয়ে crash করত।
- **`SPRING_DATASOURCE_URL` ...** → Spring Boot-এর `application.properties`-এর `spring.datasource.url` কে override করে। URL-এ host-এর নাম `db` — Compose-এ service-এর নামই hostname হিসেবে কাজ করে (`localhost` না!)।
- **`restart: on-failure`** → MySQL প্রথমবার চালু হওয়ার সময় একবার restart নেয়; সেই সময় app fail করলে আবার চেষ্টা করবে।
- `name: university-management-system` → Compose project-এর নাম।

### ১২.৩ `application.properties` (database config)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/university_db?...
spring.datasource.username=university_user
spring.datasource.password=university_pass
spring.jpa.hibernate.ddl-auto=update
```
- `ddl-auto=update` → entity দেখে table নিজে তৈরি/আপডেট করে — SQL লিখতে হয়নি।
- নিজের PC-তে চালালে `localhost`, Docker-এ compose-এর environment variable এটাকে বদলে `db` করে দেয়।

### ১২.৪ চালানোর command
```bash
docker compose up --build        # build + চালু (প্রথমবার)
# browser: http://localhost:8080
docker compose down              # বন্ধ
docker compose down -v           # বন্ধ + database মুছে ফেলা
```

---

## ১৩. Test (JUnit + MockMvc)

**ফোল্ডার:** `src/test/java/com/university/management/`

| ফাইল | কী test করে |
|---|---|
| `AuthenticationTest` | সঠিক login → role অনুযায়ী redirect; ভুল password; **pending student login rejected**; CSRF ছাড়া login 403; API তে 401 |
| `AuthorizationTest` | Teacher `/admin/**`-এ 403; Student `/teacher/**`-এ 403; শুধু Admin approve করতে পারে |
| `OwnershipTest` | Teacher অন্য Teacher edit/delete করলে 403; Student অন্য Student edit করলে 403; নিজেরটা হলে 200/204 |
| `UniquenessTest` | এক table-এর email অন্য table-এ ব্যবহার করলে 409; roll number duplicate 409 |
| `CrudAndApprovalTest` | Admin/Teacher/Student CRUD, password response-এ নেই, BCrypt hash, approve/reject workflow, validation 400, dashboard count |
| `DataSeederTest` | seed করা account দিয়ে login করা যায় |

```java
@SpringBootTest          // পুরো application চালু করে
@AutoConfigureMockMvc    // MockMvc দিয়ে server ছাড়াই HTTP request পাঠানো যায়
@ActiveProfiles("test")  // application-test.properties (H2 in-memory database)
public abstract class BaseIntegrationTest { ... }
```
```java
@Test
void teacherCannotUpdateAnotherTeacher() throws Exception {
    mockMvc.perform(put("/api/teachers/" + teacher2.getId())
                    .with(asTeacher(TEACHER1_EMAIL)).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(teacherJson("Hacked", TEACHER2_EMAIL, "")))
            .andExpect(status().isForbidden());
}
```
- Test-এ **H2** (memory database) ব্যবহার করেছি, তাই MySQL ছাড়াই test চলে। (`src/test/resources/application-test.properties`)
- এখন প্রজেক্টে মোট **৯টি test** (৬টি unit + ৩টি integration), সব pass। চালাতে: `mvn test`। (নিচের টেবিলের আগের test গুলো `old-tests/` ফোল্ডারে আছে, শুধু নিজের PC-তে, git ও CI-তে নেই।)
- নতুন ৯টি test এর বিস্তারিত ও GitHub Actions: নিচে **১৫ নম্বর অংশ**।

---

## ১৪. স্যার যে প্রশ্নগুলো করতে পারে (প্রস্তুতি)

**১. Authentication আর Authorization-এর পার্থক্য কী?**
Authentication = তুমি কে (email+password দিয়ে login)। Authorization = তুমি কী করতে পারো (role দিয়ে)। প্রথমটা `CustomUserDetailsService`, দ্বিতীয়টা `SecurityConfig`-এ।

**২. Password কীভাবে সুরক্ষিত?**
`BCryptPasswordEncoder` দিয়ে hash করে রাখি (service-এ `passwordEncoder.encode()`)। Database-এ plain text নেই; API response-এও password নেই (Response DTO)।

**৩. তিনটা table কেন আলাদা, আবার email কীভাবে unique?**
Requirement-এ তিনটা আলাদা entity। Unique constraint শুধু এক table-এ কাজ করে, তাই `EmailChecker` তিন repository-ই check করে।

**৪. Teacher কীভাবে শুধু নিজের profile edit করে?**
`TeacherService.checkOwnership()` — login করা user-এর email (`authentication.getName()`) আর record-এর email মিলিয়ে দেখে; না মিললে `AccessDeniedException` → 403। Admin হলে skip।

**৫. Student approval কীভাবে কাজ করে?**
Teacher create করলে `approved=false` (`StudentService.create`)। `CustomUserDetailsService` unapproved student-কে disabled করে → `DisabledException` → login fail ও বার্তা। Admin `approve()` করলে `approved=true`, `reject()` করলে record delete।

**৬. Session based login মানে কী?** JWT কেন নেই?
Login হলে server session বানায়, browser cookie (`JSESSIONID`) রাখে। প্রতি request-এ cookie দিয়ে user চেনা যায়। Web app-এর জন্য এটা সহজ ও standard; JWT লাগলে জটিলতা বাড়ত, requirement-এও নিষেধ ছিল।

**৭. CSRF কী, কীভাবে ঠেকিয়েছ?**
(উপরে ৭.২ দেখো) form-এ গোপন token, Spring Security যাচাই করে।

**৮. DTO কেন?** Entity সরাসরি ফেরত দিলে password বের হয়ে যেত, আর client যা খুশি field (যেমন `approved`) পাঠাতে পারত। DTO দিয়ে কী নেব/কী দেব তা নিয়ন্ত্রণ করি।

**৯. `@Autowired` নেই কেন?** `@RequiredArgsConstructor` (Lombok) + `final` field = constructor injection। এটাই modern ও পরীক্ষা করা সহজ পদ্ধতি।

**১০. JPA/Hibernate কী?** ORM — Java class ↔ database table map করে। SQL না লিখে `repository.save(student)` করলেই INSERT হয়।

**১১. `@Transactional` কী?** কয়েকটা database কাজ একসাথে — সব সফল না হলে সব rollback।

**১২. REST আর Thymeleaf দুটো কেন?** Browser user-এর জন্য HTML পেজ (Thymeleaf), অন্য app/Postman-এর জন্য JSON API। দুটোই **একই Service** ব্যবহার করে, তাই business rule একবারই লেখা — duplicate নেই।

**১৩. Docker-এ app কীভাবে MySQL-এর সাথে connect করে?** Compose একটা নিজস্ব network বানায়; service-এর নাম (`db`) hostname হয়ে যায়। তাই URL `jdbc:mysql://db:3306/university_db`।

**১৪. Healthcheck কেন দরকার?** MySQL container চালু হলেও ভেতরে database প্রস্তুত হতে সময় লাগে। `depends_on` শুধু "চালু হয়েছে" দেখে; `condition: service_healthy` দিলে "প্রস্তুত হয়েছে" পর্যন্ত অপেক্ষা করে।

**১৫. Multi-stage build-এর সুবিধা?** Final image-এ শুধু JRE + jar, তাই ছোট ও নিরাপদ।

**১৬. 403 কেন 500 হয় না?** `GlobalExceptionHandler`-এ `AccessDeniedException` আলাদা করে ধরা হয়েছে এবং Security-তে `RestAccessDeniedHandler` আছে।

**১৭. `ddl-auto=update` কী?** Hibernate entity দেখে table তৈরি/পরিবর্তন করে। (Production-এ migration tool ভালো, কিন্তু প্রজেক্টের জন্য সহজ।)

**১৮. Email বদলালে কী হয়?** Login-name যেহেতু email, নিজের email বদলালে session পুরনো থাকে — তাই `TeacherWebController.update()` session invalidate করে আবার login করতে বলে।

**১৯. Admin কি নিজেকে delete করতে পারে?** না, `AdminService.delete()` এ guard আছে (শেষ admin হারিয়ে system lock হওয়া ঠেকাতে) → 400।

**২০. একই কোড দুই জায়গায় কেন (ownership check)?** Controller-এর বোতাম লুকানো শুধু UI; কেউ সরাসরি URL/API ডাকলে আটকাতে **backend (service) check** জরুরি। তাই দুটোই আছে।

---

## ১৫. Unit test, Integration test ও GitHub Actions (CI)

### ১৫.১ Unit test আর Integration test-এর পার্থক্য (স্যারকে এভাবে বলো)
- **Unit test** = একটা ছোট অংশ (একটা class) আলাদা করে test। বাকি সব (database, password encoder) **Mockito** দিয়ে নকল (mock)। Spring চালু হয় না, তাই **খুব দ্রুত** (মিলিসেকেন্ড)।
- **Integration test** = অনেক অংশ **একসাথে** test: Spring Boot চালু, Security, Controller, Service, Database (H2) সব আসল। ধীর, কিন্তু আসল অবস্থার কাছাকাছি।

### ১৫.২ ৬টি Unit test — ফোল্ডার: `src/test/java/com/university/management/unit/`

| # | ফাইল | Test method | কী প্রমাণ করে |
|---|---|---|---|
| 1 | `StudentServiceTest` | `studentCreatedByAdminIsApprovedAndPasswordIsHashed` | Admin student বানালে `approved=true`, password hash হয়, email lowercase হয় |
| 2 | `StudentServiceTest` | `studentCreatedByTeacherIsPendingApproval` | Teacher বানালে `approved=false` (approval workflow) |
| 3 | `StudentServiceTest` | `studentCannotUpdateAnotherStudentsProfile` | অন্যের profile বদলাতে গেলে `AccessDeniedException`, আর `save()` কখনো ডাকা হয় না |
| 4 | `TeacherServiceTest` | `teacherCannotDeleteAnotherTeacherButAdminCan` | অন্য Teacher delete করতে পারে না, Admin পারে |
| 5 | `EmailCheckerTest` | `emailUsedInStudentsTableIsRejected` | email অন্য table-এ থাকলে `DuplicateResourceException` (cross-table uniqueness) |
| 6 | `CustomUserDetailsServiceTest` | `unapprovedStudentIsFoundWithStudentRoleButDisabled` | pending student-এর role `ROLE_STUDENT` কিন্তু `isEnabled() == false` |

**Mockito কোড দেখাও** (`StudentServiceTest.java`):
```java
@ExtendWith(MockitoExtension.class)          // Mockito চালু করে
class StudentServiceTest {

    @Mock private StudentRepository studentRepository;   // নকল repository, আসল database নেই
    @Mock private EmailChecker emailChecker;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private StudentService studentService;  // আসল service, ভেতরে mock বসানো

    @Test
    void studentCannotUpdateAnotherStudentsProfile() {
        Student other = new Student();
        other.setEmail("other@test.com");
        when(studentRepository.findById(5L)).thenReturn(Optional.of(other));  // "findById ডাকলে এটা দাও"
        Authentication me = new TestingAuthenticationToken("me@test.com", "pw", "ROLE_STUDENT");

        assertThrows(AccessDeniedException.class, () -> studentService.update(5L, request(), me));

        verify(studentRepository, never()).save(any());       // save() ডাকা হয়নি — নিশ্চিত
    }
}
```
- `@Mock` = নকল object। `@InjectMocks` = যে class test করছি তার ভেতরে mock গুলো বসায়।
- `when(...).thenReturn(...)` = mock-কে বলি কী উত্তর দেবে। `verify(..., never())` = নির্দিষ্ট method ডাকা হয়নি কিনা দেখে।
- `TestingAuthenticationToken` = login করা user-এর নকল (নাম + role)।

### ১৫.৩ ৩টি Integration test — ফাইল: `src/test/java/com/university/management/EndToEndIntegrationTest.java`
প্রতিটা test একটা **পুরো গল্প** test করে (একটা URL না):

| # | Test method | গল্প |
|---|---|---|
| 1 | `teacherRegistersStudentAdminApprovesThenStudentCanLogIn` | Teacher API-তে student register করে → student login করতে গিয়ে **pending** বার্তা পায় → Admin pending page-এ দেখে ও approve করে → এখন student login করে `/student/dashboard`-এ যায় |
| 2 | `sessionKeepsUserLoggedInUntilLogout` | সত্যিকারের login → session cookie দিয়ে dashboard খোলে → কিন্তু `/admin/dashboard` এ **403** → logout করলে session শেষ |
| 3 | `studentWhoChangesOwnEmailIsLoggedOutAndMustUseNewEmail` | Student নিজের email বদলায় → logout হয়ে `/login?emailChanged` → পুরনো email-এ login **fail**, নতুন email-এ **সফল** |

```java
class EndToEndIntegrationTest extends BaseIntegrationTest {   // BaseIntegrationTest = H2 database + test data

    @Test
    void sessionKeepsUserLoggedInUntilLogout() throws Exception {
        MockHttpSession session = sessionOf(login(TEACHER1_EMAIL, PASSWORD)...);

        mockMvc.perform(get("/teacher/dashboard").session(session))    // শুধু session cookie
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isForbidden());                      // role ঠিক না → 403
        ...
        assertThat(session.isInvalid()).isTrue();                        // logout-এর পর session শেষ
    }
}
```
- `@SpringBootTest` (BaseIntegrationTest-এ) = পুরো application চালু। `MockMvc` = browser ছাড়াই HTTP request পাঠায়।
- আগের test গুলো `user("...")` শর্টকাট দিয়ে "login করা" ভান করত; এই নতুন test গুলো **আসল login + আসল session** ব্যবহার করে।

### ১৫.৪ GitHub Actions (CI) — ফাইল: `.github/workflows/ci.yml`
**CI (Continuous Integration) কী?** কেউ কোড push করলেই GitHub-এর server নিজে থেকে কোড নামিয়ে build করে ও সব test চালায়। কোনো test fail করলে সাথে সাথে লাল ❌ — ভাঙা কোড কেউ খেয়াল না করে merge করতে পারে না।

```yaml
name: CI
on:
  push:            # যেকোনো branch-এ push করলে
  pull_request:    # এবং প্রতিটা Pull Request-এ

jobs:
  test:
    runs-on: ubuntu-latest            # GitHub-এর Linux machine
    steps:
      - uses: actions/checkout@v4     # কোড নামাও
      - uses: actions/setup-java@v4   # Java 17 (Temurin) + Maven cache
        with: { distribution: temurin, java-version: '17', cache: maven }

      - name: Unit tests              # ৬টি unit test
        run: mvn -B test -Dtest='com.university.management.unit.*Test'

      - name: Integration tests       # ৩টি নতুন + আগের integration test
        run: mvn -B test -Dtest='!com.university.management.unit.*Test'

      - uses: actions/upload-artifact@v4   # test report জমা রাখো (fail হলেও)
        if: always()
        with: { name: surefire-reports, path: target/surefire-reports }
```
- `on: push / pull_request` → trigger। `runs-on` → কোন machine। `steps` → একটার পর একটা কাজ।
- `-Dtest='...'` → Maven-কে বলে কোন test চালাবে। `!` মানে "এগুলো বাদে"।
- `mvn -B` = batch mode (CI-এর জন্য পরিষ্কার log)।
- Test-এ **H2 in-memory database** ব্যবহার হয়, তাই CI-তে MySQL বসাতে হয় না।
- দেখবে কোথায়: GitHub repo → **Actions** ট্যাব → সর্বশেষ run → সবুজ ✅।

### ১৫.৫ GitHub-এ push করা
```bash
git init -b main
git add -A
git commit -m "Initial commit: University Management System"
git remote add origin https://github.com/Azmaeen113/universitymanagement.git
git push -u origin main
```
- Commit-এর author **Azmaeen113** (তোমার GitHub account), তাই Contributors-এ শুধু তোমার নামই আসে।
- `.gitignore`-এ `target/` আছে, তাই build-এর ফাইল push হয় না।

### স্যার জিজ্ঞেস করতে পারে
- **Mock কেন?** যাতে শুধু ওই class-এর logic test হয়; database বা অন্য class ভুল করলে এই test fail না করে। আর দ্রুত চলে।
- **Unit test আর integration test দুটোই কেন?** Unit test দ্রুত ও নির্দিষ্টভাবে ভুল ধরে; integration test প্রমাণ করে অংশ গুলো একসাথে ঠিকঠাক কাজ করছে (Security + Controller + Service + DB)।
- **CI কেন?** প্রতিবার হাতে test চালাতে ভুলে যেতে পারি; CI নিজে চালায়, তাই কোড সবসময় সচল থাকে।
- **Test-এ MySQL লাগে না কেন?** H2 in-memory database ব্যবহার (`application-test.properties`)।

---

## ১৬. প্রেজেন্টেশনের ক্রম (সাজেস্টেড)

1. `context.md` → requirement বলো।
2. `pom.xml` → dependency (Lombok, JPA, Security, Thymeleaf…)।
3. Entity → Repository → DTO → Service → Controller (layer-wise)।
4. `SecurityConfig` + `CustomUserDetailsService` → login ও role।
5. Live demo: `docker compose up --build` → Admin দিয়ে login → Pending list → Approve → Teacher দিয়ে login → অন্য teacher edit করতে গেলে 403।
6. API demo: curl/Postman।
7. `mvn test` চালিয়ে ৯টি pass দেখাও, তারপর GitHub-এর **Actions** ট্যাবে সবুজ ✅ দেখাও।
8. `Dockerfile` ও `compose.yaml` ব্যাখ্যা।

> **টিপ:** Demo-র আগে একবার `docker compose up --build` দিয়ে দেখে নাও (প্রথমবার ইন্টারনেট লাগবে — Maven library ও MySQL image download হবে)।

শুভকামনা! 🎓
