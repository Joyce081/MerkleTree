import { createStore } from 'vuex'

export default createStore({
  state: {
    auth: {
      isAuthenticated: false,
      user: null,
      token: null
    }
  },
  mutations: {
    SET_AUTH(state, { user, token }) {
      state.auth.isAuthenticated = true
      state.auth.user = user
      state.auth.token = token
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    CLEAR_AUTH(state) {
      state.auth.isAuthenticated = false
      state.auth.user = null
      state.auth.token = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    // 添加初始化认证的 mutation
    INITIALIZE_AUTH(state) {
      const token = localStorage.getItem('token')
      const user = JSON.parse(localStorage.getItem('user'))
      if (token && user) {
        state.auth.isAuthenticated = true
        state.auth.user = user
        state.auth.token = token
      }
    }
  },
  actions: {
    login({ commit }, { user, token }) {
      commit('SET_AUTH', { user, token });
    },
    logout({ commit }) {
      commit('CLEAR_AUTH');
    },
    initializeAuth({ commit }) {
      commit('INITIALIZE_AUTH');
    }
  },
  getters: {
    currentUser: state => state.auth.user,
    isAuthenticated: state => state.auth.isAuthenticated
  }
})