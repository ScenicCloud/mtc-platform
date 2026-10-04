const { createApp, ref, computed, onMounted } = Vue

createApp({
  setup() {
    // ========== 页面状态 ==========
    const currentPage = ref('login') // login | register | dashboard
    const currentUser = ref('')

    // ========== 登录 ==========
    const loginForm = ref({ username: '', password: '' })
    const loginErrors = ref({ username: '', password: '', general: '' })

    const validateLogin = () => {
      loginErrors.value = { username: '', password: '', general: '' }
      let valid = true
      if (!loginForm.value.username.trim()) {
        loginErrors.value.username = '请输入用户名'
        valid = false
      }
      if (!loginForm.value.password) {
        loginErrors.value.password = '请输入密码'
        valid = false
      }
      return valid
    }

    const handleLogin = () => {
      if (!validateLogin()) return

      const users = JSON.parse(localStorage.getItem('todoapp_users') || '[]')
      const user = users.find(
        (u) => u.username === loginForm.value.username && u.password === loginForm.value.password
      )

      if (user) {
        localStorage.setItem('todoapp_current_user', user.username)
        currentUser.value = user.username
        currentPage.value = 'dashboard'
        loadTodos()
      } else {
        loginErrors.value.general = '用户名或密码错误'
      }
    }

    // ========== 注册 ==========
    const registerForm = ref({ username: '', password: '', confirmPassword: '' })
    const registerErrors = ref({ username: '', password: '', confirmPassword: '', general: '' })

    const validateRegister = () => {
      registerErrors.value = { username: '', password: '', confirmPassword: '', general: '' }
      let valid = true

      const uname = registerForm.value.username.trim()
      if (!uname) {
        registerErrors.value.username = '请输入用户名'
        valid = false
      } else if (uname.length < 3) {
        registerErrors.value.username = '用户名至少 3 个字符'
        valid = false
      } else if (uname.length > 20) {
        registerErrors.value.username = '用户名不能超过 20 个字符'
        valid = false
      }

      if (!registerForm.value.password) {
        registerErrors.value.password = '请输入密码'
        valid = false
      } else if (registerForm.value.password.length < 6) {
        registerErrors.value.password = '密码至少 6 个字符'
        valid = false
      }

      if (!registerForm.value.confirmPassword) {
        registerErrors.value.confirmPassword = '请确认密码'
        valid = false
      } else if (registerForm.value.password !== registerForm.value.confirmPassword) {
        registerErrors.value.confirmPassword = '两次输入的密码不一致'
        valid = false
      }

      return valid
    }

    const handleRegister = () => {
      if (!validateRegister()) return

      const users = JSON.parse(localStorage.getItem('todoapp_users') || '[]')
      if (users.some((u) => u.username === registerForm.value.username.trim())) {
        registerErrors.value.general = '该用户名已被注册'
        return
      }

      users.push({
        username: registerForm.value.username.trim(),
        password: registerForm.value.password,
        createdAt: new Date().toISOString(),
      })
      localStorage.setItem('todoapp_users', JSON.stringify(users))

      // 注册成功，跳转到登录页
      loginForm.value.username = registerForm.value.username.trim()
      loginForm.value.password = ''
      registerForm.value = { username: '', password: '', confirmPassword: '' }
      currentPage.value = 'login'
    }

    // ========== 页面跳转 ==========
    const goToRegister = () => {
      registerErrors.value = { username: '', password: '', confirmPassword: '', general: '' }
      currentPage.value = 'register'
    }

    const goToLogin = () => {
      loginErrors.value = { username: '', password: '', general: '' }
      currentPage.value = 'login'
    }

    const handleLogout = () => {
      localStorage.removeItem('todoapp_current_user')
      currentUser.value = ''
      todos.value = []
      currentPage.value = 'login'
    }

    // ========== 待办列表 ==========
    const todos = ref([])
    const newTodo = ref('')
    const filter = ref('all') // all | active | completed
    const todoError = ref('')

    const loadTodos = () => {
      const key = `todoapp_todos_${currentUser.value}`
      todos.value = JSON.parse(localStorage.getItem(key) || '[]')
    }

    const saveTodos = () => {
      const key = `todoapp_todos_${currentUser.value}`
      localStorage.setItem(key, JSON.stringify(todos.value))
    }

    const addTodo = () => {
      const text = newTodo.value.trim()
      if (!text) {
        todoError.value = '请输入待办内容'
        return
      }
      if (text.length > 100) {
        todoError.value = '待办内容不能超过 100 个字符'
        return
      }
      todoError.value = ''
      todos.value.unshift({
        id: Date.now(),
        text,
        completed: false,
        createdAt: new Date().toISOString(),
      })
      newTodo.value = ''
      saveTodos()
    }

    const toggleTodo = (id) => {
      const todo = todos.value.find((t) => t.id === id)
      if (todo) {
        todo.completed = !todo.completed
        saveTodos()
      }
    }

    const deleteTodo = (id) => {
      todos.value = todos.value.filter((t) => t.id !== id)
      saveTodos()
    }

    const clearCompleted = () => {
      todos.value = todos.value.filter((t) => !t.completed)
      saveTodos()
    }

    const filteredTodos = computed(() => {
      if (filter.value === 'active') return todos.value.filter((t) => !t.completed)
      if (filter.value === 'completed') return todos.value.filter((t) => t.completed)
      return todos.value
    })

    const allCount = computed(() => todos.value.length)
    const activeCount = computed(() => todos.value.filter((t) => !t.completed).length)
    const completedCount = computed(() => todos.value.filter((t) => t.completed).length)

    const emptyStateText = computed(() => {
      if (filter.value === 'active') return '暂无进行中的待办'
      if (filter.value === 'completed') return '暂无已完成的待办'
      return '还没有待办事项，添加一个吧！'
    })

    // ========== 初始化 ==========
    onMounted(() => {
      // 初始化一个演示用户（如果不存在）
      const users = JSON.parse(localStorage.getItem('todoapp_users') || '[]')
      if (!users.some((u) => u.username === 'demo')) {
        users.push({
          username: 'demo',
          password: 'demo123',
          createdAt: new Date().toISOString(),
        })
        localStorage.setItem('todoapp_users', JSON.stringify(users))
      }

      // 检查登录状态
      const savedUser = localStorage.getItem('todoapp_current_user')
      if (savedUser) {
        currentUser.value = savedUser
        currentPage.value = 'dashboard'
        loadTodos()
      }
    })

    return {
      currentPage,
      currentUser,
      // login
      loginForm,
      loginErrors,
      handleLogin,
      goToRegister,
      // register
      registerForm,
      registerErrors,
      handleRegister,
      goToLogin,
      // dashboard
      todos,
      newTodo,
      filter,
      todoError,
      filteredTodos,
      allCount,
      activeCount,
      completedCount,
      emptyStateText,
      addTodo,
      toggleTodo,
      deleteTodo,
      clearCompleted,
      handleLogout,
    }
  },
}).mount('#app')
