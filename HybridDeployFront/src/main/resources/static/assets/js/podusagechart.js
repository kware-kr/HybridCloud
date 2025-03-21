{
  // 전역 변수: 현재까지 열린 팝업 중 가장 높은 z-index (팝업 포커스 관리용)
  let highestZ = 1000;
  let popupCount = 0;
  // 팝업을 요청정보(mlid_poduid) 기준으로 관리할 객체
  let popupMap = {};

  /**
   * 동적 폰트 크기를 계산하는 함수
   * @param {object} context - Chart.js context (chart 포함)
   * @param {number} baseSize - 기본 폰트 크기 (600px 기준)
   * @param {number} maxSize - 최대 폰트 크기 제한
   * @returns {number} - 계산된 폰트 크기
   */
  function dynamicFontSize(context, baseSize, maxSize) {
    let calculatedSize = baseSize * (context.chart.width / 600);
    return Math.min(calculatedSize, maxSize);
  }

  /**
   * GPU 색상 할당 함수
   * @param {string|number} key - GPU 고유 키
   * @returns {string} - 선택된 색상
   */
  function getColorForGPU(key) {
    const colors = ['purple', 'orange', 'teal', 'magenta', 'brown'];
    let idx = parseInt(key) % colors.length;
    return colors[idx];
  }

  /**
   * createPopup() 함수
   * 테이블의 요청정보(mlid, poduid, collectDt)를 받아, 동일 mlid와 poduid에 해당하는 팝업이 이미 존재하면  
   * 그 팝업을 활성화하고, 없으면 새로 생성합니다.
   * @param {string} mlid 
   * @param {string} poduid 
   * @param {string} podname 
   */
  function createPopup(mlid, poduid, podname, page) {
    // 고유 키는 mlid와 poduid 조합 (collectDt는 키에서 배제)
    let key = page + "_" + mlid + "_" + poduid;
    
    // 이미 해당 키의 팝업이 존재하면, 해당 팝업을 최상위로 올리고  
    // 테이블의 해당 버튼에도 active 클래스를 추가한 후 종료
    if (popupMap[key]) {
      highestZ++;
      popupMap[key].css("z-index", highestZ);
      $(".popup").removeClass("active");
      popupMap[key].addClass("active");
      return;
    }
    
    // 새 팝업 생성
    popupCount++;
    let canvasId = "chartCanvas_" + popupCount;
    
    let podname_title = "";
    if(podname != '')
      podname_title = `<br>POD 요청명: ${podname}`;
    // 팝업 생성: 요청정보를 제목에 표시 (collectDt도 표시)
    let popup = $(`
      <div class="popup" id="popup_${popupCount}">
        <div class="popup-header">
          <div>MLID: ${mlid}${podname_title}</div>
          <div class="button-group">
            <span class="button popup-zoom"><i class="bi bi-arrows-angle-expand"></i></span>
            <span class="button popup-reset"><i class="bi bi-arrow-counterclockwise"></i></span>
            <span class="button popup-fullscreen"><i class="bi bi-fullscreen"></i></span>
            <span class="button popup-close"><i class="bi bi-x"></i></span>
          </div>
        </div>
        <div class="popup-content">
          <canvas id="${canvasId}"></canvas>
        </div>
      </div>
    `);
    
    $("#popupContainer").append(popup);
    popupMap[key] = popup;

    //신규팝업을 최상위로
    if(popup){
      highestZ++;
      popupMap[key].css("z-index", highestZ);
      $(".popup").removeClass("active");
      popupMap[key].addClass("active");
    }
    
    // 팝업 클릭 시, active 클래스 추가하여 시각적으로 활성화된 창임을 표시하고,  
    // 테이블의 해당 버튼에도 active 클래스를 부여
    popup.on("mousedown", function() {
      $(".popup").removeClass("active");
      $(this).addClass("active");
      highestZ++;
      $(this).css("z-index", highestZ);
      $(".popupBtn").each(function() {
        let $card = $(this).closest(".card");
        let btnKey = $card.data("mlid") + "_" + $card.data("poduid");
        if (btnKey === key) {
          $(".popupBtn").removeClass("active");
          $(this).addClass("active");
        }
      });
    });
    
    // 팝업 격자 배치
    const popupWidth = 600, popupHeight = 400, margin = 20;
    const containerWidth = $(window).width();
    let count = $("#popupContainer .popup").length;
    let index = count - 1;
    let maxPerRow = Math.floor(containerWidth / (popupWidth + margin)) || 1;
    let row = Math.floor(index / maxPerRow), col = index % maxPerRow;
    popup.css({ left: (col * (popupWidth + margin)) + "px", top: (row * (popupHeight + margin)) + "px" });
    
    // 드래그 설정 (팝업 헤더를 핸들로)
    popup.draggable({ handle: ".popup-header", containment: "window" });
    
    // GPU 데이터셋 저장용 객체 초기화
    popup.data("gpuIndDatasets", {});
    
    // 리사이즈 설정
    popup.resizable({
      handles: 'n, e, s, w, ne, se, sw, nw',
      alsoResize: `#${canvasId}`,
      start: function() { $(this).css("transition", "none"); },
      stop: function() { $(this).css("transition", "none"); },
      resize: function(event, ui) {
        let chartInst = popup.data("chartInstance");
        if (chartInst) {
          let newWidth = ui.size.width;
          function getThickness(width) {
            if (width < 100) return { borderWidth: 0.3, pointRadius: 0.3 };
            else if (width < 200) return { borderWidth: 0.5, pointRadius: 0.5 };
            else if (width < 300) return { borderWidth: 0.8, pointRadius: 0.8 };
            else if (width < 400) return { borderWidth: 1, pointRadius: 1 };
            else return { borderWidth: 1.3, pointRadius: 1.3 };
          }
          let thickness = getThickness(newWidth);
          chartInst.data.datasets.forEach(dataset => {
            dataset.borderWidth = thickness.borderWidth;
            dataset.pointRadius = thickness.pointRadius;
            dataset.pointHoverRadius = thickness.pointRadius * 1.5;
          });

          // 범례의 폰트 크기를 동적으로 업데이트
          if (chartInst.options.plugins.legend && chartInst.options.plugins.legend.labels) {
            chartInst.options.plugins.legend.labels.font.size = dynamicFontSize({ chart: { width: newWidth } }, 8, 12);
          }
          
          // 차트 타이틀 폰트 크기도 동적으로 업데이트 (필요에 따라)
          if (chartInst.options.plugins.title) {
            chartInst.options.plugins.title.font.size = dynamicFontSize({ chart: { width: newWidth } }, 10, 14);
          }
          
          // 일정 너비 이하에서는 일부 요소 숨기기
          let threshold = 500;
          if (newWidth < threshold) {
            chartInst.options.plugins.legend.display = false;
            chartInst.options.plugins.title.display = false;
            if (chartInst.options.scales.x) {
              chartInst.options.scales.x.ticks.display = false;
            }
            if (chartInst.options.scales.yCpu) {
              chartInst.options.scales.yCpu.ticks.display = false;
              chartInst.options.scales.yCpu.title.display = false;
            }
            if (chartInst.options.scales.yMemory) {
              chartInst.options.scales.yMemory.ticks.display = false;
              chartInst.options.scales.yMemory.title.display = false;
            }
            if (chartInst.options.scales.yNetwork) {
              chartInst.options.scales.yNetwork.ticks.display = false;
              chartInst.options.scales.yNetwork.title.display = false;
            }
            if (chartInst.options.scales.yGpu) {
              chartInst.options.scales.yGpu.ticks.display = false;
              chartInst.options.scales.yGpu.title.display = false;
            }
          } else {
            chartInst.options.plugins.legend.display = true;
            chartInst.options.plugins.title.display = true;
            if (chartInst.options.scales.x) {
              chartInst.options.scales.x.ticks.display = true;
            }
            if (chartInst.options.scales.yCpu) {
              chartInst.options.scales.yCpu.ticks.display = true;
              chartInst.options.scales.yCpu.title.display = true;
            }
            if (chartInst.options.scales.yMemory) {
              chartInst.options.scales.yMemory.ticks.display = true;
              chartInst.options.scales.yMemory.title.display = true;
            }
            if (chartInst.options.scales.yNetwork) {
              chartInst.options.scales.yNetwork.ticks.display = true;
              chartInst.options.scales.yNetwork.title.display = true;
            }
            if (chartInst.options.scales.yGpu) {
              chartInst.options.scales.yGpu.ticks.display = true;
              chartInst.options.scales.yGpu.title.display = true;
            }
          }
          chartInst.resize();
          chartInst.update();
        }
      }
    });
    
    // 닫기 버튼: 팝업 닫을 때, 인터벌 정리 후 DOM에서 제거하고 popupMap에서 해당 키 삭제
    popup.find(".popup-close").click(function() {
      let chartInst = popup.data("chartInstance");
      if(chartInst) {
        chartInst.destroy();
      }
      clearInterval(popup.data("intervalId"));
      popup.remove();
      delete popupMap[key];
      rearrangePopups();
    });
    
    // 확대 버튼: 현재 크기의 1.2배 확대
    popup.find(".popup-zoom").click(function() {
      if ($(this).hasClass("disabled")) return;
      let currentWidth = popup.width();
      let currentHeight = popup.height();
      let newWidth = currentWidth * 1.2;
      let newHeight = currentHeight * 1.2;
      let winWidth = $(window).width();
      let winHeight = $(window).height();
      if (newWidth >= winWidth || newHeight >= winHeight) {
        popup.data("prevSize", { width: currentWidth, height: currentHeight });
        popup.data("prevPos", { left: popup.css("left"), top: popup.css("top") });
        popup.css({
          left: "0px",
          top: "0px",
          width: winWidth + "px",
          height: winHeight + "px",
          position: "fixed"
        });
        popup.find(".popup-fullscreen").html('<i class="bi bi-fullscreen-exit"></i>');
        popup.data("fullscreen", true);
        $(this).addClass("disabled").css("opacity", "0.5");
        updateChartFonts(popup, winWidth);
        return;
      }
      popup.animate({ width: newWidth, height: newHeight }, 300, function() {
        popup.resizable("option", "alsoResize", `#${canvasId}`);
        updateChartFonts(popup, newWidth);
        popup.trigger("resize");
      });
    });
    
    // 원래 크기로 버튼: 전체화면 상태이면 전체화면 해제, 아니면 기본 크기(600x400) 복원
    popup.find(".popup-reset").click(function() {
      if (popup.data("fullscreen")) {
        popup.find(".popup-fullscreen").trigger("click");
      } else {
        popup.animate({ width: 600, height: 400 }, 300, function() {
          let currentWidth = popup.width();
          let currentHeight = popup.height();

          popup.resizable("option", "alsoResize", `#${canvasId}`);
          updateChartFonts(popup, currentWidth);
          popup.trigger("resize");
        });
      }
    });
    
    // 전체화면 버튼: 전체화면 토글
    popup.find(".popup-fullscreen").click(function() {
      let isFullscreen = popup.data("fullscreen") || false;
      if (!isFullscreen) {
        popup.data("prevSize", { width: popup.width(), height: popup.height() });
        popup.data("prevPos", { left: popup.css("left"), top: popup.css("top") });
        let winWidth = $(window).width();
        let winHeight = $(window).height();
        popup.css({
          left: "0px",
          top: "0px",
          width: winWidth + "px",
          height: winHeight + "px",
          position: "fixed"
        });
        $(this).html('<i class="bi bi-fullscreen-exit"></i>');
        popup.data("fullscreen", true);
        popup.find(".popup-zoom").addClass("disabled").css("opacity", "0.5");
        updateChartFonts(popup, winWidth);
      } else {
        let prevSize = popup.data("prevSize");
        let prevPos = popup.data("prevPos");
        popup.animate({
          left: prevPos.left,
          top: prevPos.top,
          width: prevSize.width,
          height: prevSize.height
        }, 300, function() {
          popup.css("position", "absolute");
          popup.data("fullscreen", false);
          popup.trigger("resize");
          updateChartFonts(popup, prevSize.width);
        });
        $(this).html('<i class="bi bi-fullscreen"></i>');
        popup.find(".popup-zoom").removeClass("disabled").css("opacity", "1");
      }
      popup.trigger("resize");
    });
    
    // Chart.js 초기화: 기본 데이터셋 (CPU, Memory, Network)
    // Total GPU Usage는 GPU 데이터가 나타날 때 동적으로 추가합니다.
    let ctx = document.getElementById(canvasId).getContext('2d');
    let chartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: [],
        datasets: [
          { label: 'CPU Usage', data: [], borderColor: 'red', fill: false, tension: 0.1, yAxisID: 'yCpu', borderWidth: 1.3, pointRadius: 1.3 },
          { label: 'Memory Usage (MB)', data: [], borderColor: 'blue', fill: false, tension: 0.1, yAxisID: 'yMemory', borderWidth: 1.3, pointRadius: 1.3 },
          { label: 'Network Usage (KB)', data: [], borderColor: 'green', fill: false, tension: 0.1, yAxisID: 'yNetwork', borderWidth: 1.3, pointRadius: 1.3 }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: { font: { size: 8 }, boxWidth: 10 }
          },
          title: { display: true, text: 'Resource Usage Over Time', font: { size: 10 } },
          tooltip: {
            callbacks: {
              // GPU 데이터셋이면 rawValues 배열의 원본 문자열 표시
              label: function(context) {
                let dataset = context.dataset;
                if (dataset.rawValues) {
                  return dataset.label + ': ' + dataset.rawValues[context.dataIndex];
                }
                return dataset.label + ': ' + context.formattedValue;
              }
            }
          }
        },
        scales: {
          x: {
            title: { display: false },
            ticks: {
              autoSkip: true,
              autoSkipPadding: 10,
              font: {
                size: function(context) {
                  let baseSize = 6;
                  let calculatedSize = baseSize * (context.chart.width / 600);
                  return Math.min(calculatedSize, 10);
                }
              },
              callback: function(value, index, ticks) {
                let label = this.getLabelForValue(value);
                let currDate = new Date(label);
                function formatFull(date) {
                  let m = date.getMonth() + 1;
                  let d = date.getDate();
                  let h = date.getHours();
                  let min = date.getMinutes();
                  h = h < 10 ? '0' + h : h;
                  min = min < 10 ? '0' + min : min;
                  return m + '.' + d + ' ' + h + ':' + min;
                }
                function formatTime(date) {
                  let h = date.getHours();
                  let min = date.getMinutes();
                  h = h < 10 ? '0' + h : h;
                  min = min < 10 ? '0' + min : min;
                  return h + ':' + min;
                }
                if (index === 0 || index === ticks.length - 1) {
                  return formatFull(currDate);
                }
                let prevLabel = this.getLabelForValue(ticks[index - 1].value);
                let prevDate = new Date(prevLabel);
                if (currDate.toDateString() !== prevDate.toDateString()) {
                  return formatFull(currDate);
                }
                return formatTime(currDate);
              },
              display: true
            }
          },
          yCpu: {
            type: 'linear',
            position: 'left',
            title: { 
              display: true, 
              text: 'CPU Usage', 
              font: { size: dynamicFontSize({ chart: { width: 600 } }, 8, 12) }
            },
            ticks: { font: { size: function(context) { return dynamicFontSize(context, 8, 12); } }, display: true }
          },
          yMemory: {
            type: 'linear',
            position: 'right',
            title: { 
              display: true, 
              text: 'Memory Usage (MB)', 
              font: { size: dynamicFontSize({ chart: { width: 600 } }, 8, 12) }
            },
            grid: { drawOnChartArea: false },
            ticks: { font: { size: function(context) { return dynamicFontSize(context, 8, 12); } }, display: true }
          },
          yNetwork: {
            type: 'linear',
            position: 'left',
            title: { 
              display: true, 
              text: 'Network Usage (KB)', 
              font: { size: dynamicFontSize({ chart: { width: 600 } }, 8, 12) }
            },
            grid: { drawOnChartArea: false },
            ticks: { font: { size: function(context) { return dynamicFontSize(context, 8, 12); } }, display: true },
            offset: true
          },
          yGpu: {
            type: 'linear',
            position: 'right',
            title: { 
              display: true, 
              text: 'GPU Usage', 
              font: { size: dynamicFontSize({ chart: { width: 600 } }, 8, 12) }
            },
            grid: { drawOnChartArea: false },
            ticks: { font: { size: function(context) { return dynamicFontSize(context, 8, 12); } }, display: true },
            offset: true
          }
        }
      }
    });
    
    // Chart.js 인스턴스를 팝업 데이터에 저장
    popup.data("chartInstance", chartInstance);
    popup.data("lastTimestamp", null);
    
    // GPU 개별 데이터셋 저장용 객체 초기화
    popup.data("gpuIndDatasets", {});
    
    // --- 데이터 fetch 관련 코드 ---
    // 초기 호출 시 POST로 mlId와 podUid를 보내고, 응답에서 마지막 collectDt를 lastCollectDt에 저장
    let lastCollectDt = null; // 팝업 내부에서 관리하는 변수
    /**
     * fetchPodUsageAndUpdate 함수
     * mlId와 podUid를 POST 방식으로 전송하며, 
     * (lastCollectDt가 있으면 함께 보내고)
     * 응답의 records 배열을 사용해 차트를 업데이트합니다.
     * 만약 응답의 statusPhase가 "RUNNING"이 아니면 타이머를 중단합니다.
     */
    function fetchPodUsageAndUpdate() {
      let params = { mlId: mlid, podUid: poduid };
      if (lastCollectDt) {
        params.collectDt = lastCollectDt;
      }
      
      fetch('/workload/podusage', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params)
      })
      .then(response => response.json())
      .then(data => {
        console.log("Fetched pod usage data:", data);
        if (data.result && data.result.length > 0) {
          let records = data.result;
          
          records.forEach(record => {
            let idx = chartInstance.data.labels.length;
            // x축 레이블 추가
            chartInstance.data.labels.push(record.collectDt);
            // CPU, Memory, Network 데이터 추가
            chartInstance.data.datasets[0].data.push(record.usageCpu);
            chartInstance.data.datasets[1].data.push((record.usageMemory / (1024 * 1024)).toFixed(2));
            chartInstance.data.datasets[2].data.push((record.usageNetwork / 1024).toFixed(2));
            let integrated = 0;
            
            if (typeof record.usageGpu === 'string') {
              try {
                record.usageGpu = JSON.parse(record.usageGpu);
              } catch (e) {
                console.error("Error parsing usageGpu:", record.usageGpu, e);
                record.usageGpu = {};
              }
            }
            
            // GPU 개별 데이터 업데이트
            for (let key in record.usageGpu) {
              let rawVal = record.usageGpu[key];
              let numericVal = parseFloat(rawVal) || 0;
              integrated += numericVal;
              let gpuIndDatasets = popup.data("gpuIndDatasets");
              if (gpuIndDatasets[key] === undefined) {
                // GPU 개별 데이터셋 추가
                let newDataset = {
                  label: "GPU " + key,
                  data: Array(idx).fill(null),
                  borderColor: getColorForGPU(key),
                  fill: false,
                  tension: 0.1,
                  yAxisID: 'yGpu',
                  borderWidth: 1.3,
                  pointRadius: 1.3,
                  pointHoverRadius: 1.95,
                  rawValues: []
                };
                chartInstance.data.datasets.push(newDataset);
                gpuIndDatasets[key] = chartInstance.data.datasets.length - 1;
                popup.data("gpuIndDatasets", gpuIndDatasets);
              }
              let dsIndex = popup.data("gpuIndDatasets")[key];
              chartInstance.data.datasets[dsIndex].data.push(numericVal);
              if (!chartInstance.data.datasets[dsIndex].rawValues) {
                chartInstance.data.datasets[dsIndex].rawValues = [];
              }
              chartInstance.data.datasets[dsIndex].rawValues.push(rawVal);
            }
            
            // GPU 데이터셋에 없는 키에는 null 추가
            let gpuIndDatasets = popup.data("gpuIndDatasets");
            for (let key in gpuIndDatasets) {
              if (!(key in record.usageGpu)) {
                let dsIndex = popup.data("gpuIndDatasets")[key];
                chartInstance.data.datasets[dsIndex].data.push(0); //null->0
              }
            }
            
            // Total GPU Usage 업데이트
            // GPU 데이터가 하나라도 있으면 integrated는 0보다 크거나(혹은 0일 수도 있으므로)
            // GPU 정보가 비어있다면 integrated는 그대로 0이고, 이 경우 빈 객체라면 모든 키가 없으므로 Object.keys().length === 0
            if (Object.keys(record.usageGpu).length > 0) {
              // Total GPU Usage 데이터셋이 없으면 동적으로 추가 (이전 시점에 null로 채워줌)
              let totalGpuIndex = chartInstance.data.datasets.findIndex(ds => ds.label === "Total GPU Usage");
              if (totalGpuIndex === -1) {
                // 기존 3개 데이터셋(0,1,2)의 길이에 맞춰 null을 채운 후 추가 
                let initialData = Array(idx).fill(0); //null->0
                initialData.push(integrated);
                chartInstance.data.datasets.push({
                  label: "Total GPU Usage",
                  data: initialData,
                  borderColor: 'black',
                  fill: false,
                  tension: 0.1,
                  yAxisID: 'yGpu',
                  borderWidth: 1.3,
                  pointRadius: 1.3
                });
              } else {
                chartInstance.data.datasets[totalGpuIndex].data.push(integrated);
              }
            } else {
              // GPU 정보가 없으면
              let totalGpuIndex = chartInstance.data.datasets.findIndex(ds => ds.label === "Total GPU Usage");
              if (totalGpuIndex !== -1) {
                chartInstance.data.datasets[totalGpuIndex].data.push(null);
              }
            }
          });
          chartInstance.update();
          
          // 완료 상태 처리: 마지막 레코드의 statusPhase가 RUNNING이 아니면
          if (records[records.length - 1].statusPhase !== "RUNNING") {
            // 만약 Total GPU Usage 데이터셋이 존재하는데 모든 값이 null이라면 제거
            let totalGpuIndex = chartInstance.data.datasets.findIndex(ds => ds.label === "Total GPU Usage");
            if (totalGpuIndex !== -1) {
              let dataArray = chartInstance.data.datasets[totalGpuIndex].data;
              if (dataArray.every(val => val === null)) {
                chartInstance.data.datasets.splice(totalGpuIndex, 1);
              }
            }
            clearInterval(intervalId);
            return;
          }
          // 마지막 collectDt 갱신
          lastCollectDt = records[records.length - 1].collectDt;
        }
      })
      .catch(error => console.error("Pod usage fetch error:", error));
    }

    // 최초 호출
    fetchPodUsageAndUpdate();

    // 1분마다 업데이트: fetchPodUsageAndUpdate 함수를 호출
    let intervalId = setInterval(fetchPodUsageAndUpdate, 60000);
    
    // 인터벌 ID를 팝업 데이터에 저장 (닫을 때 정리하기 위함)
    popup.data("intervalId", intervalId);
  }

  /**
   * 팝업 재배치 함수
   * - 화면 너비에 맞춰 팝업들을 격자 형태로 재배치
   */
  function rearrangePopups() {
    const popupWidth = 600, popupHeight = 400, margin = 20;
    const containerWidth = $(window).width();
    let popups = $("#popupContainer .popup");
    let maxPerRow = Math.floor(containerWidth / (popupWidth + margin)) || 1;
    popups.each(function(index) {
      let row = Math.floor(index / maxPerRow);
      let col = index % maxPerRow;
      $(this).animate({ left: (col * (popupWidth + margin)) + "px", top: (row * (popupHeight + margin)) + "px" }, 300);
    });
  }

  function updateChartFonts(popup, newWidth) {
    let chartInst = popup.data("chartInstance");
    if (!chartInst) return;
    if (chartInst.options.plugins.legend && chartInst.options.plugins.legend.labels) {
      chartInst.options.plugins.legend.labels.font.size = dynamicFontSize({ chart: { width: newWidth } }, 8, 11);
    }
    if (chartInst.options.plugins.title) {
      chartInst.options.plugins.title.font.size = dynamicFontSize({ chart: { width: newWidth } }, 10, 14);
    }
    chartInst.resize();
    chartInst.update();
  }

  $(document).ready(function() {
    //중복 바인딩이 되는 부분을 제거하기위해 off 이후 on으로 처리함
    $(document).off("click", ".popupBtn").on("click", ".popupBtn", function() {
      let mlid = $(this).data("mlid");
      let poduid = $(this).data("poduid");
      let podname = $(this).data("podname");
      let page = $(this).data("page");
      $(".popupBtn").removeClass("active");
      $(this).addClass("active");
      createPopup(mlid, poduid, podname, page);
    });
  });
}
