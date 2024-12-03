/**
* Template Name: NiceAdmin
* Template URL: https://bootstrapmade.com/nice-admin-bootstrap-admin-html-template/
* Updated: Apr 20 2024 with Bootstrap v5.3.3
* Author: BootstrapMade.com
* License: https://bootstrapmade.com/license/
*/

/**
 * addEventListener을 활용한 핸들러 등록은 DOM 외의 영역에 등록되므로 삭제가 어렵다. 그래서 중복 등록이 된다.
 * onclick 처럼 onxxx 엘리먼트의 속성으로 등록한 이벤트는 DOM에서 속성을 찾아서 제거가 가능하다.
 * 
 * 이를 위해서 전역으로 등록 관리하는 레지스트리를 만들어서 중복등록 되지 않도록 한다. 
 */
	  
const eventRegistry = new Map();

 
  const on = (type, el, listener, all = false) => {
      const key = `${el}_${type}`;  // 엘리먼트와 이벤트 타입을 키로 설정

      // 이미 등록된 이벤트 핸들러가 있으면 제거
      const existingListener = eventRegistry.get(key);
      if (existingListener) {
		  if (all) {
			  select(el, all).forEach(e => e.removeEventListener(type, existingListener));
		  } else {
			  select(el, all).removeEventListener(type, existingListener);
		  }
	  }

      // 새 이벤트 핸들러 등록
      if (all) {
          select(el, all).forEach(e => e.addEventListener(type, listener));
      } else {
          select(el, all).addEventListener(type, listener);
      }

      // 이벤트 핸들러 정보 저장
      eventRegistry.set(key, listener);
  };

  // 이벤트 핸들러 제거
  const removeEventHandler = (type, el) => {
      const key = `${el}_${type}`;
      const listener = eventRegistry.get(key);
      if (listener) {
          select(el).removeEventListener(type, listener);
          eventRegistry.delete(key);
      }
  };

/*  
// 부모에서 요소를 제거하고 복사본을 삽입하는 방법은 돔의 참조를 이용해서 이벤트가 등록되는데, 참초를 제거하고, 새로운 참조를 등록하여
돔이 변경된 것처럼 한다, 즉 실제 event를 지우는 것이 아니라 객체를 변경하는 경우
속성으로 등록된 이벤트는 계속 유지
  const removeAllEventListeners = (el, type) => {
      const clone = el.cloneNode(true);
	  if(el.parentNode){
      	el.parentNode.replaceChild(clone, el);  // 부모에서 요소를 제거하고 복사본을 삽입
	      clone.addEventListener(type, function() {
	        console.log("클린업 후 새 이벤트 등록");
	      });
	  }
    };*/
	
  /**
   * Easy selector helper function
   */
  const select = (el, all = false) => {
    el = el.trim()
    if (all) {
      return [...document.querySelectorAll(el)]
    } else {
      return document.querySelector(el)
    }
  }
  

(function() {
  "use strict";

  
 

  /**
   * Easy event listener function
   */
/*  const on = (type, el, listener, all = false) => {
    if (all) {
      select(el, all).forEach(e => e.addEventListener(type, listener))
    } else {
      select(el, all).addEventListener(type, listener)
    }
  }*/

  /**
   * Easy on scroll event listener 
   */
  const onscroll = (el, listener) => {
	//removeAllEventListeners(el, 'scroll');
    el.addEventListener('scroll', listener)
  }

  /**
   * Sidebar toggle
   */
  if (select('.toggle-sidebar-btn')) {
	/*const t = select('.toggle-sidebar-btn');
	if (t) {
	  removeAllEventListeners(t, 'click');
	}*/
	  
    on('click', '.toggle-sidebar-btn', function(e) {
      select('body').classList.toggle('toggle-sidebar')
    })
  }

  /**
   * Search bar toggle
   */
  if (select('.search-bar-toggle')) {
	/*const t = select('.search-bar-toggle');
	if (t) {
	  removeAllEventListeners(t, 'click');
	}*/
		
    on('click', '.search-bar-toggle', function(e) {
      select('.search-bar').classList.toggle('search-bar-show')
    })
  }

  /**
   * Navbar links active state on scroll
   */
  let navbarlinks = select('#navbar .scrollto', true)
  const navbarlinksActive = () => {
    let position = window.scrollY + 200
    navbarlinks.forEach(navbarlink => {
      if (!navbarlink.hash) return
      let section = select(navbarlink.hash)
      if (!section) return
      if (position >= section.offsetTop && position <= (section.offsetTop + section.offsetHeight)) {
        navbarlink.classList.add('active')
      } else {
        navbarlink.classList.remove('active')
      }
    })
  }
  window.addEventListener('load', navbarlinksActive)
  onscroll(document, navbarlinksActive)

  /**
   * Toggle .header-scrolled class to #header when page is scrolled
   */
  let selectHeader = select('#header')
  if (selectHeader) {
    const headerScrolled = () => {
      if (window.scrollY > 100) {
        selectHeader.classList.add('header-scrolled')
      } else {
        selectHeader.classList.remove('header-scrolled')
      }
    }
    window.addEventListener('load', headerScrolled)
    onscroll(document, headerScrolled)
  }

  /**
   * Back to top button
   */
  let backtotop = select('.back-to-top')
  if (backtotop) {
    const toggleBacktotop = () => {
      if (window.scrollY > 100) {
        backtotop.classList.add('active')
      } else {
        backtotop.classList.remove('active')
      }
    }
    window.addEventListener('load', toggleBacktotop)
    onscroll(document, toggleBacktotop)
  }
})();




/*
showNotification('This is a success notification!', 'success'); // 녹색 배경
showNotification('This is a warning notification!', 'warning'); // 노란색 배경
showNotification('This is an info notification!', 'info'); // 하늘색 배경
showNotification('This is a danger notification!', 'danger'); // 빨간색 배경
showNotification('This is a light notification!', 'light'); // 연한 회색 배경
showNotification('This is a dark notification!', 'dark'); // 검은색 배경
*/
function showNotification(message, type = "primary") {
  // 알림을 동적으로 생성할 HTML 내용
  const notihtml = `
    <div aria-live="polite" aria-atomic="true" class="position-fixed bottom-0 end-0 p-3" style="z-index: 1100;">
      <!-- Toast Notification -->
      <div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
          <div class="d-flex">
              <div class="toast-body">
                  ${message}
              </div>
              <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
          </div>
      </div>
    </div>
  `;

  // 새로운 div 요소 생성
  const topElement = document.createElement("div");
  topElement.innerHTML = notihtml.trim(); // innerHTML에 HTML을 삽입

  // #main 엘리먼트 아래에 동적으로 추가
  const mainElement = document.getElementById("main");
  mainElement.appendChild(topElement);

  // 알림 요소를 찾고 Bootstrap Toast 초기화
  const toastElement = topElement.querySelector(".toast");

  // 부트스트랩 Toast 초기화 및 표시
  const toast = new bootstrap.Toast(toastElement);
  toast.show();

  // 알림이 일정 시간 후에 자동으로 제거되도록 설정 (예: 3초 후)
  setTimeout(() => {
    topElement.remove(); // 알림 제거
  }, 2000);
}